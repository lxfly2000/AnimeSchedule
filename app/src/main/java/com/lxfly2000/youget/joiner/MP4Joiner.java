package com.lxfly2000.youget.joiner;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class MP4Joiner extends Joiner {
    @Override
    public int join(String[]inputs,String output){
        //参考：https://github.com/sannies/mp4parser/blob/master/examples/src/main/java/com/googlecode/mp4parser/AppendExample.java
        ArrayList<Movie>inputMovies=new ArrayList<>();
        try {
            for (String path : inputs) {
                inputMovies.add(MovieCreator.build(path));
            }
            ArrayList<Track> videoTracks = new ArrayList<>();
            ArrayList<Track> audioTracks = new ArrayList<>();
            for (Movie m : inputMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun"))
                        audioTracks.add(t);
                    if (t.getHandler().equals("vide"))
                        videoTracks.add(t);
                }
            }
            Movie outputMovie = new Movie();
            if (!audioTracks.isEmpty()) {
                outputMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (!videoTracks.isEmpty()) {
                outputMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
            Container outputFile = new DefaultMp4Builder().build(outputMovie);

            FileChannel fc = new RandomAccessFile(output, "rw").getChannel();
            outputFile.writeContainer(fc);
            fc.close();
        }catch (IOException e){
            return -1;
        }
        return 0;
    }

    @Override
    public String getExt() {
        return "mp4";
    }
}
