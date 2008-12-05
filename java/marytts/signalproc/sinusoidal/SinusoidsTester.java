/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.sinusoidal;

import java.io.IOException;
import java.util.Arrays;

import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * This class can be used to generate various sinusoid signals and writing them to wav and ptc files
 * to be used in testing the analysis/synthesis algorithms
 * 
 * @author Oytun T&uumlrk
 */
public class SinusoidsTester extends BaseTester{
    public static float DEFAULT_PHASE = 0.0f;
    public static int DEFAULT_FRAME_INDEX = 0;
    
    public SinusoidsTester(float freqInHz)
    {
        this(freqInHz, DEFAULT_AMP);
    }
    
    public SinusoidsTester(float freqInHz, float amp)
    {
        this(freqInHz, amp, DEFAULT_PHASE);
    }
    
    public SinusoidsTester(float freqInHz, float amp, float phaseInDegrees)
    {
        this(freqInHz, amp, phaseInDegrees, DEFAULT_FRAME_INDEX);
    }
    
    public SinusoidsTester(float freqInHz, float amp, float phaseInDegrees, int frameIndex)
    {
        this(freqInHz, amp, phaseInDegrees, frameIndex, DEFAULT_DUR);
    }
    
    public SinusoidsTester(float freqInHz, float amp, float phaseInDegrees, int frameIndex, float durationInSeconds)
    {
        this(freqInHz, amp, phaseInDegrees, frameIndex, durationInSeconds, DEFAULT_FS);
    }
    
    public SinusoidsTester(Sinusoid sin)
    {
        this(sin, DEFAULT_DUR);
    }
    
    public SinusoidsTester(Sinusoid sin, float durationInSeconds)
    {
        this(sin, durationInSeconds, DEFAULT_FS);
    }
    
    public SinusoidsTester(float freqInHz, float amp, float phaseInDegrees, int frameIndex, float durationInSeconds, int samplingRateInHz)
    {
        super();
        
        Sinusoid [] tmpSins = new Sinusoid[1];
        tmpSins[0] = new Sinusoid(amp, freqInHz, phaseInDegrees, frameIndex);
        init(tmpSins, durationInSeconds, samplingRateInHz);
    }
    
    public SinusoidsTester(Sinusoid sin, float durationInSeconds, int samplingRateInHz)
    {
        super();
        
        Sinusoid [] tmpSins = new Sinusoid[1];
        tmpSins[0] = new Sinusoid(sin);
        init(tmpSins, durationInSeconds, samplingRateInHz);
    }
    
    public SinusoidsTester(Sinusoid [] sinsIn)
    {
        this(sinsIn, DEFAULT_DUR);
    }
    
    public SinusoidsTester(Sinusoid [] sinsIn, float durationInSeconds)
    {
        this(sinsIn, durationInSeconds, DEFAULT_FS);
    }
    
    public SinusoidsTester(Sinusoid [] sinsIn, float durationInSeconds, int samplingRateInHz)
    {
        super();
        
        init(sinsIn, durationInSeconds, samplingRateInHz);
    }
    
    //These constructors can be used to create tracks starting and terminating at desired time instants    
    public SinusoidsTester(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds)
    {
        this(sinsIn, startTimesInSeconds, endTimesInSeconds, DEFAULT_FS);
    }
    
    public SinusoidsTester(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds, int samplingRateInHz)
    {
        super();
        
        init(sinsIn, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
    }
    //
    
    public void init(Sinusoid [] sinsIn, float durationInSeconds, int samplingRateInHz)
    {
        if (sinsIn.length>0)
        {
            float [] startTimesInSeconds = new float[sinsIn.length];
            float [] endTimesInSeconds = new float[sinsIn.length];
            for (int i=0; i<sinsIn.length; i++)
            {
                startTimesInSeconds[i] = 0.0f;
                endTimesInSeconds[i] = durationInSeconds;
            }
            
            init(sinsIn, startTimesInSeconds, endTimesInSeconds, samplingRateInHz);
        } 
    }
    
    public void init(Sinusoid [] sinsIn, float [] startTimesInSeconds, float [] endTimesInSeconds, int samplingRateInHz)
    {
        fs = samplingRateInHz;
        signal = null;
        pitchMarks = null;
        int i, j;
        
        if (sinsIn!=null)
        {
            assert startTimesInSeconds.length==endTimesInSeconds.length;
            assert sinsIn.length==endTimesInSeconds.length;
            
            float minFreq = 2*fs;
            int minFreqInd = -1;
            for (i=0; i<sinsIn.length; i++)
            {
                if (sinsIn[i].freq>0.0f && sinsIn[i].freq<minFreq)
                {
                    minFreq = sinsIn[i].freq;
                    minFreqInd = i;
                }
            }
            
            int [] startSampleIndices = new int[sinsIn.length];
            int [] endSampleIndices = new int[sinsIn.length];
            
            for (i=0; i<startTimesInSeconds.length; i++)
            {
                if (startTimesInSeconds[i]<0.0f)
                    startTimesInSeconds[i]=0.0f;
                if (endTimesInSeconds[i]<0.0f)
                    endTimesInSeconds[i]=0.0f;
                if (startTimesInSeconds[i]>endTimesInSeconds[i])
                    startTimesInSeconds[i] = endTimesInSeconds[i];
                
                startSampleIndices[i] = (int)(Math.floor(startTimesInSeconds[i]*fs+0.5));
                endSampleIndices[i] = (int)(Math.floor(endTimesInSeconds[i]*fs+0.5))-1;
            }
            
            //int minStartSampleIndex = MathUtils.getMin(startSampleIndices);
            int minStartSampleIndex = 0; //To ensure pitch marks being generated starting from 0th sample
            int maxEndSampleIndex = MathUtils.getMax(endSampleIndices);
            
            //Create pitch marks by finding the longest period
            int maxT0;
            
            if (minFreqInd>=0)
                maxT0 = (int)(Math.floor(fs/minFreq+0.5));
            else //No non-zero Hz sinusoids found, therefore set maxT0 to a fixed number
                maxT0 = (int)Math.floor(0.010f*fs+0.5);
            
            int numPitchMarks = (int)(Math.floor(((double)(maxEndSampleIndex-minStartSampleIndex+1))/maxT0+0.5)) + 1; 
            pitchMarks = new int[numPitchMarks];
            for (i=0; i<numPitchMarks; i++)
                pitchMarks[i] = Math.min(i*maxT0+minStartSampleIndex, maxEndSampleIndex);
            //
            
            f0s = SignalProcUtils.pitchMarks2PitchContour(pitchMarks, ws, ss, fs);
            
            if (maxEndSampleIndex>0)
            {
                signal = new double[maxEndSampleIndex+1];
                Arrays.fill(signal, 0.0);

                //Synthesize sinusoids
                for (i=0; i<sinsIn.length; i++)
                {
                    for (j=startSampleIndices[i]; j<endSampleIndices[i]; j++)
                        signal[j] += sinsIn[i].amp * Math.sin(MathUtils.TWOPI*((j-startSampleIndices[i])*(sinsIn[i].freq/fs) + sinsIn[i].phase/360.0));
                }  
            }
        }
    }
    
    public static void main(String[] args) throws IOException
    {
        int i, numSins;
        float [] tStarts, tEnds;
        SinusoidsTester s = null;
        
        //Single sinusoid, time-invariant
        s = new SinusoidsTester(200.0f);
        //

        //Several sinusoids, time-invariant
        numSins = 1;
        Sinusoid [] sins = new Sinusoid[numSins];
        tStarts = new float[numSins];
        tEnds = new float[numSins];
        sins[0] = new Sinusoid(100.0f, 400.0f, 0.0f); tStarts[0] = 0.0f; tEnds[0] = 1.5f;
        //sins[1] = new Sinusoid(25.0f, 211.0f, 0.0f); tStarts[1] = 0.0f; tEnds[1] = 1.5f;
        //sins[2] = new Sinusoid(125.0f, 555.0f, 0.0f); tStarts[2] = 0.0f; tEnds[2] = 1.5f;
        //sins[3] = new Sinusoid(110.0f, 917.0f, 0.0f); tStarts[3] = 0.0f; tEnds[3] = 1.5f;
        //sins[4] = new Sinusoid(100.0f, 175.0f, 0.0f); tStarts[4] = 1.3f; tEnds[4] = 2.5f;
        //sins[5] = new Sinusoid(25.0f, 346.0f, 0.0f); tStarts[5] = 1.3f; tEnds[5] = 2.5f;
        //sins[6] = new Sinusoid(125.0f, 981.0f, 0.0f); tStarts[6] = 2.0f; tEnds[6] = 3.0f;
        //sins[7] = new Sinusoid(70.0f, 1317.0f, 0.0f); tStarts[7] = 2.0f; tEnds[7] = 3.5f;
        s = new SinusoidsTester(sins, tStarts, tEnds);
        //
        
        /*
        //Sinus part
        numSins = 10;
        float [] sinFreqs = new float[numSins];
        sinFreqs[0] = 180.0f;
        sinFreqs[1] = 380.0f;
        sinFreqs[2] = 580.0f;
        sinFreqs[3] = 780.0f;
        
        Sinusoid [] sins = new Sinusoid[numSins];
        for (i=0; i<numSins; i++)
            sins[i] = new Sinusoid(100.0f, i*300+100, 0.0f);
        
        s = new SinusoidsTester(sins);
        //
        */
        
        
        /*
        //Fixed sinusoidal track with a gap
        numSins = 4;
        Sinusoid [] sins = new Sinusoid[numSins];
        tStarts = new float[numSins];
        tEnds = new float[numSins];
        
        sins[0] = new Sinusoid(0.0f, 0.0f, 0.0f);
        tStarts[0] = 0.0f;
        tEnds[0] = 0.1f; 
        sins[1] = new Sinusoid(100.0f, 200.0f, 0.0f);
        tStarts[1] = 0.1f;
        tEnds[1] = 0.2f; 
        sins[2] = new Sinusoid(100.0f, 300.0f, 0.0f);
        tStarts[2] = 0.3f;
        tEnds[2] = 0.4f;
        sins[3] = new Sinusoid(0.0f, 0.0f, 0.0f);
        tStarts[3] = 0.4f;
        tEnds[3] = 0.5f;
        s = new SinusoidsTester(sins, tStarts, tEnds);
        //
         */
        
        s.write(args[0], args[1]);
    }
}
