package jode.test;

class A {
}

interface I1 {
}

interface I2 {
}

interface I12 extends I1, I2 {
}

class B extends A implements I1 {
}

class C extends B implements I2, I12 {
}

class D extends A implements I12 {
}

class E extends A implements I2 {
}

public class LocalTypes {
    A a;
    B b;
    C c;
    D d;
    E e;

    I1 i1;
    I2 i2;
    I12 i12;

    boolean g_bo;
    byte g_by;
    short g_sh;
    int g_in;

    int z;
    int[]ain;

    public void arithTest() {
        int a=1,b=2;
        boolean x = true,y = false;
        int c=0;
        arithTest();
        if (x & y) {
            c = 5;
            arithTest();
            x &= y;
            arithTest();
            x = x | y;
            arithTest();
            x ^= y;
            arithTest();
            x = x && y;
            arithTest();
            b <<= a;
            b <<= c;
        }
        a&=b;
    }
    
    public void intTypeTest() {
        boolean b = false;
        boolean abo[] = null;
        byte aby[] = null;
        byte by;
        int in;
        short sh;
        b = g_bo;
        in = g_sh;
        sh = (short)g_in;
        by = (byte)sh;
        sh = by;
        in = by;
        abo[0] = g_bo;
        abo[1] = false;
        abo[2] = true;
        aby[0] = g_by;
        aby[1] = 0;
        aby[2] = 1;
    }

    native void arrFunc(B[] b);
    
    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultType () {
        B myB = c;
	C myC = c;
        I2 myI2 = c;
        I12 myI12 = c;
	boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);

	while (bool) {
            if (bool) {
                i1 = myB;
                i2 = myC;
                i1 = aB[0];
            } else if (bool) {
                i1 = myI12;
                i2 = myI2;
            } else {
                i1 = myC;
                i2 = myI12;
            }
	    myB = b;
            if (bool)
                myI2 = i12;
            else {
                myI12 = d;
                myI2 = e;
            }
	}
    }

    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultArrayType () {
        boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);
        C[][][][][] x = new C[4][3][4][][];
        int[][][][][] y = new int[1][2][][][];

	while (bool) {
            if (bool) {
                i1 = aB[0];
                aB[0] = b;
            }
	}
    }
}

