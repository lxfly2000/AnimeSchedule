#二维XML矩阵转置

import xml.dom.minidom

dom=xml.dom.minidom.parse('app/src/main/res/layout/activity_test_availability.xml')

def getDom(a,b):
    root=dom.getElementsByTagName('TableLayout')[0]
    row=root.getElementsByTagName('TableRow')[a]
    if a==0:
        if b==0:
            return row.getElementsByTagName('Space')[b]
        else:
            return row.getElementsByTagName('TextView')[b-1]
    return row.getElementsByTagName('TextView')[b]

for i in range(0,8):
    print('<TableRow>')
    for j in range(0,4):
        print(getDom(j,i).toxml())
    print('</TableRow>')
