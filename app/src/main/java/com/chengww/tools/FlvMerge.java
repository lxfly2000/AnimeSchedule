//https://gitee.com/chengww5217/BiliBiliMerge/blob/master/src/com/chengww/tools/FlvMerge.java

package com.chengww.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FlvMerge {
    private final static int FLV_HEADER_SIZE = 9;
    private final static int FLV_TAG_HEADER_SIZE = 11;
    private final static int MAX_DATA_SIZE = 16777220;
    private static MyTimeStamp lastTimeStamp;

    public void merge(File[] files,File path) throws IOException {
        String inputFileName, mergeFileName;

        List<FLVContext> mergeTaskList = new ArrayList<FLVContext>();
        Date date = new Date();
        long ti = date.getTime();

        //合并列表只有一个文件
        if(files == null || files.length == 0){
            System.out.println("File[] files 为空，跳过合并");
            return;
        }else if (files.length == 1) {
            FileOutputStream writer = new FileOutputStream(path);
            byte[] buffer = new byte[1024];
            int readcount;
            FileInputStream reader = new FileInputStream(files[0]);
            while ((readcount = reader.read(buffer)) != -1) {
                writer.write(buffer);
            }
            writer.flush();
            writer.close();
            reader.close();
            System.out.println(path.getName() + "复制成功（单文件）");
            return;
        } else {
            for (int i = 0; i < files.length; i++) {
                inputFileName = files[i].getAbsolutePath();
                FLVContext flvCtx = new FLVContext();
                if (InitFLVContext(flvCtx, inputFileName) != 0) {
                    System.out.printf("视频合并初始化失败 %s!\n",
                            inputFileName);
                    System.exit(1);
                } else {
                    //成功初始化
                    System.out.printf("视频合并：成功初始化\n");
                    mergeTaskList.add(flvCtx);
                }

            }
        }

        mergeFileName = path.getAbsolutePath();

        System.out.println("开始合并"+path.getName());
        if (do_merge_tasks(mergeTaskList, mergeFileName) == 0) {
            System.out.println("-----" + path.getName() + "合并成功!------\n");
            ti = new Date().getTime() - ti;
            System.out.printf("耗时 %fs\n", ti / 1000000.0);
        } else {
            System.out.println("合并失败!!!\n");
        }

    }

    private int do_merge_tasks(List<FLVContext> mergeTaskList,
                               String mergeFileName) throws IOException {
        File mergeFile = new File(mergeFileName);

        lastTimeStamp = new MyTimeStamp();

        // insure the flv files are suitable to merge
        boolean firstOne = true;
        FLVContext curCtx = null;
        int n = 1;
        for (FLVContext nextFLV : mergeTaskList) {
            if (firstOne) {
                curCtx = nextFLV;
                firstOne = false;
                continue;
            }
            if (!IsSuitableToMerge(curCtx, nextFLV)) {
                System.out.println("第"+n+"个视频信息不同，不可合并");
                n++;
            } else {
                System.out.println("第"+n+"个视频可以合并");
                n++;
            }
            curCtx = nextFLV;

        }

        //获得输出流
        FileOutputStream out = new FileOutputStream(mergeFile);
        DataOutputStream dos = new DataOutputStream(out);

        // combine them one by one
        firstOne = true;

        System.out.printf("开始添加FileData.first..\n");
        FLVContext firstFLV = mergeTaskList.get(0);

        System.out.println(firstFLV.count);

        if (AddFileData(firstFLV.fileSource, dos, true, lastTimeStamp) != 0) {
            System.exit(-1);
        }
        for (FLVContext nextFLV : mergeTaskList) {
            if (firstOne) {
                firstOne = false;
                continue;
            }

            System.out.println("开始FileData...");
            System.out.println(nextFLV.count);
            if (AddFileData(nextFLV.fileSource, dos, false, lastTimeStamp) != 0) {
                System.exit(-1);
            }


        }

        dos.close();
        UpdateDuration(mergeFileName);
        return 0;
    }

    public static int ByteIndexOf(byte[] src, byte[] find, int start)
    {
        boolean matched = false;
        int end = find.length - 1;
        int skip = 0;
        for (int index = start; index <= src.length - find.length; ++index)
        {
            matched = true;
            if (find[0] != src[index] || find[end] != src[index + end]) continue;
            else skip++;
            if (end > 10)
                if (find[skip] != src[index + skip] || find[end - skip] != src[index + end - skip])
                    continue;
                else skip++;
            for (int subIndex = skip; subIndex < find.length - skip; ++subIndex)
            {
                if (find[subIndex] != src[index + subIndex])
                {
                    matched = false;
                    break;
                }
            }
            if (matched)
            {
                return index;
            }
        }
        return -1;
    }

    private void UpdateDuration(String mergeFile) {
        try {
            double durationSum=0.0;
            RandomAccessFile raf = new RandomAccessFile(mergeFile, "rwd");
            //Hack:修改第一个ScriptTag中的AMF为duration的数值，这只是为那些制作不规范的播放器作的一个临时解决方案
            //参考：https://www.jianshu.com/p/7ffaec7b3be6
            //参考：https://blog.csdn.net/tianyue168/article/details/5994962
            //可知“duration”后接类型代码为0（仅占一个字节），数据长度为8字节，为double类型
            //需要将所有出现的“duration”数值累加起来输出至第一个“duration”中

            long firstDurationPos=0;
            //跳过FLV头部9字节
            raf.seek(FLV_HEADER_SIZE);
            while(true){
                long posCurrentPreTagSize=raf.getFilePointer();
                if(posCurrentPreTagSize+4>=raf.length())
                    break;
                //卧槽我才发现Java原来默认采用的是BigEndian模式！？
                int prevTagLength=raf.readInt();
                //各tag部分
                //tagHeader占11字节
                int tagDataSize=raf.readInt();
                int tagType=(tagDataSize>>24)&0xFF;
                tagDataSize=tagDataSize&0xFFFFFF;
                if(tagType==18) {//脚本Tag
                    raf.seek(raf.getFilePointer()+FLV_TAG_HEADER_SIZE-4);
                    //现在文件指针在tagData区
                    //搜索到“duration”后读它后面的1个字节即为长度的类型，根据类型确定要读取的字节数
                    long posTagData=raf.getFilePointer();
                    byte[]tagData=new byte[tagDataSize];
                    raf.read(tagData,0,tagDataSize);
                    //TODO:虽然时间显示没问题了，但是在某些渣渣播放器比如三星视频上，拖动还是有问题，可能还需要调整其他参数
                    int indexDur=ByteIndexOf(tagData,"duration".getBytes(),0);
                    if(indexDur!=-1){
                        byte valueType=tagData[indexDur+8];
                        indexDur+=9;
                        if(valueType==0){
                            byte[]durationBytes=new byte[8];
                            for(int i=0;i<durationBytes.length;i++){
                                durationBytes[i]=tagData[indexDur+i];
                            }
                            durationSum+= QWordToDouble(durationBytes);
                            if(firstDurationPos==0){
                                firstDurationPos=posTagData+indexDur;
                            }
                        }
                    }
                }
                raf.seek(posCurrentPreTagSize+4+FLV_TAG_HEADER_SIZE+tagDataSize);
            }
            if(firstDurationPos!=0){//找到了duration
                raf.seek(firstDurationPos);
                byte[]duration_bytes= DoubleToQWord(durationSum);
                raf.write(duration_bytes);
            }
            raf.close();
        }catch (IOException e){/*Ignore*/}
    }

    private double QWordToDouble(byte[]data){
        try {
            DataInputStream ois = new DataInputStream(new ByteArrayInputStream(data,0,8));
            return ois.readDouble();
        }catch (IOException e){
            return 0.0;
        }
    }

    private byte[] DoubleToQWord(double d){
        try{
            ByteArrayOutputStream baos=new ByteArrayOutputStream(8);
            DataOutputStream oos=new DataOutputStream(baos);
            oos.writeDouble(d);
            oos.flush();
            return baos.toByteArray();
        }catch (IOException e){
            return null;
        }
    }

    private int AddFileData(File fileSource, DataOutputStream dos,
                            boolean isFirstFile, MyTimeStamp lastTimestamp) throws IOException {
        int readLen;
        int dataSize;
        MyTimeStamp curTimestamp = new MyTimeStamp();
        MyTimeStamp newTimestamp = new MyTimeStamp();
        byte[] tmp = new byte[20];
        byte[] buf = new byte[MAX_DATA_SIZE];

        FileInputStream in = new FileInputStream(fileSource);
        DataInputStream dis = new DataInputStream(in);

        // 如果是第一个文件，那么就把第一个文件的头部写到目标文件里面
        if (isFirstFile) {
            System.out.println("开始dis.read.........");
            if (FLV_HEADER_SIZE + 4 == dis.read(tmp, 0, FLV_HEADER_SIZE + 4)) {
                dos.write(tmp, 0, FLV_HEADER_SIZE + 4);
                dos.flush();
            }
        } else {
            System.out.println("非第一个文件，跳过头部");

            if (dis.skip(FLV_HEADER_SIZE + 4) != FLV_HEADER_SIZE + 4) {
                System.out.println("failed~~");
                System.exit(-1);
            }
        }

        while (ReadFromFile(dis, tmp, FLV_TAG_HEADER_SIZE) > 0) {
            dataSize = FromInt24StringBe(tmp, 1);
            //System.out.printf("\ndataSize::::%d\n", dataSize);

			/*for(int i = 4; i < 8;i++)
				System.out.printf("%x,",tmp[i]);*/

            curTimestamp.setTimeStamp(GetTimestamp(tmp, 4));
            newTimestamp.setTimeStamp(curTimestamp.getTimeStamp()
                    + lastTimestamp.getTimeStamp());
            SetTimestamp(tmp, 4, newTimestamp.getTimeStamp());


            if (WriteToFile(dos, tmp, FLV_TAG_HEADER_SIZE) < 0) {
                dos.close();
                dis.close();
                System.exit(-1);
            }

            readLen = dataSize + 4;

            if (ReadFromFile(dis, buf, readLen) > 0) {
                // System.out.printf("\nReadFromFile(dis, buf, readLen) > 0\n");
                if (WriteToFile(dos, buf, readLen) < 0) {
                    dos.close();
                    dis.close();
                    System.exit(-1);
                }
            } else {
                dos.close();
                dis.close();
                System.exit(-1);
            }

        }

        // update the timestamp and return
        lastTimestamp.setTimeStamp(newTimestamp.getTimeStamp());

        System.out.println(lastTimestamp.getTimeStamp());
        dis.close();
        return 0;

    }

    public int ReadFromFile(DataInputStream dis, byte[] buffer, int size)
            throws IOException {
        int readLen, realReadLen;
        byte[] tmp;

        readLen = size;
        realReadLen = 0;
        tmp = buffer;

        int readNum = 0;// 已经读取的字节数

        while ((realReadLen = dis.read(tmp, readNum, readLen)) > 0) {
            readLen -= realReadLen;
            readNum += realReadLen;
        }
        return (readLen == 0) ? size : -1;

    }

    public static int WriteToFile(DataOutputStream dos, byte[] buffer, int size)
            throws IOException {
        int writeLen;
        byte[] tmp = null;
        writeLen = size;
        tmp = buffer;

        /*
         * while ( dos.size() < size) { dos.write(tmp, dos.size(), writeLen); }
         */

        // System.out.print("running writetofile...\n");

        dos.write(tmp, 0, writeLen);
        dos.flush();
        // return (dos.size() == size)?size:-1;

        return 1;
    }

    private boolean IsSuitableToMerge(FLVContext flvCtx1,
                                      FLVContext flvCtx2) {
        return (flvCtx1.soundFormat == flvCtx2.soundFormat)
                && (flvCtx1.soundRate == flvCtx2.soundRate)
                && (flvCtx1.soundSize == flvCtx2.soundSize)
                && (flvCtx1.soundType == flvCtx2.soundType)
                && (flvCtx1.videoCodecID == flvCtx2.videoCodecID);
    }

    private int InitFLVContext(FLVContext flvCtx, String inputFileName)
            throws IOException {

        flvCtx.fileSource = new File(inputFileName);

//		if (!IsFLVFile(flvCtx.fileSource)) {
//			System.out.printf("%s: invalid FLV file!", inputFileName);
//			System.exit(1);
//		}
        if (GetFLVFileInfo(flvCtx) != 0) {
            System.out.println("cannot find flv file info!");
            System.exit(1);
        }

        return 0;
    }

    private int GetFLVFileInfo(FLVContext flvCtx) throws IOException {
        boolean hasAudioParams, hasVideoParams;
        int skipSize;
        int dataSize;
        int tagType;
        byte[] tmp = new byte[FLV_TAG_HEADER_SIZE + 1];
        if (flvCtx == null)
            return -1;
        skipSize = 9;
        hasVideoParams = hasAudioParams = false;

        FileInputStream in = new FileInputStream(flvCtx.fileSource);
        DataInputStream dis = new DataInputStream(in);
        dis.skip(skipSize);// 跳9个字节

        System.out.printf("此文件的File类型大小：%d\n", flvCtx.fileSource.length());
        skipSize = 4;
        while (!hasVideoParams || !hasAudioParams) {
            dis.skip(skipSize);// 跳四个字节
            if (dis.read(tmp, 0, FLV_TAG_HEADER_SIZE + 1) != FLV_TAG_HEADER_SIZE + 1) {
                dis.close();
                return -1;
            }

            tagType = tmp[0] & 0x1f;
            System.out.printf("tagType:%d\n", tagType);

            switch (tagType) {
                case 8:
                    flvCtx.soundFormat = (tmp[FLV_TAG_HEADER_SIZE] & 0xf0) >> 4;
                    flvCtx.soundRate = (tmp[FLV_TAG_HEADER_SIZE] & 0x0c) >> 2;
                    flvCtx.soundSize = (tmp[FLV_TAG_HEADER_SIZE] & 0x02) >> 1;
                    flvCtx.soundType = (tmp[FLV_TAG_HEADER_SIZE] & 0x01) >> 0;
                    hasAudioParams = true;

                    System.out.printf("%s\n", flvCtx.toString());
                    break;
                case 9:
                    flvCtx.videoCodecID = (tmp[FLV_TAG_HEADER_SIZE] & 0x0f);
                    hasVideoParams = true;
                    break;
                default:
                    break;
            }

            // System.out.printf("%x,%x,%x,%x\n",tmp[0] & 0xff,tmp[1] &
            // 0xff,tmp[2] & 0xff,tmp[3] & 0xff);

            dataSize = FromInt24StringBe(tmp, 1);

            skipSize = dataSize - 1 + 4;

            System.out.printf("数据大小%d\n", dataSize);
        }

        dis.close();

        return 0;

    }


    private int FromInt24StringBe(byte[] str, int i) {
        return (str[i] & 0xff) << 16 | (str[i + 1] & 0xff) << 8 | str[i + 2]
                & 0xff;
    }

    private long GetTimestamp(byte[] tmp, int i) {

        return (
                (tmp[i + 3] << 24 & 0xFF000000) |
                        (tmp[i] << 16 & 0xFF0000) |
                        (tmp[i + 1] << 8 & 0xFF00) |
                        (tmp[i + 2] & 0xFF));
    }

    private void SetTimestamp(byte[] tmp, int i, long newTimestamp) {
        tmp[i + 3] = (byte)(newTimestamp >> 24);
        tmp[i] =  (byte)(newTimestamp >> 16);
        tmp[i + 1] = (byte)(newTimestamp >> 8);
        tmp[i + 2] = (byte)(newTimestamp);
    }

}

class MyTimeStamp {
    public long timeStamp;

    public MyTimeStamp() {
        timeStamp = 0;
    }

    public MyTimeStamp(long time) {
        timeStamp = time;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

}

class FLVContext {

    int soundFormat;
    int soundRate;
    int soundSize;
    int soundType;
    int videoCodecID;

    File fileSource;
    FLVContext next;
    int count;

    public String toString() {
        return "\nsoundFormat:" + this.soundFormat + "\nsoundRate:"
                + this.soundRate + "\nsoundSize:" + this.soundType
                + "\nsoundType:" + this.soundType + "\nvideoCodeId:"
                + this.videoCodecID + "\n";
    }

}