package rohan.samplers;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class SamplerTest {

    @BeforeClass
    public static void before() {
        //Paths.makeDir(paths.freqItemsetsDirPath);
    }

    @AfterClass
    public static void after() {
        //Paths.deleteDir(paths.resultsDir);
    }

    @Test
    public void same() throws IOException {
        Sampler[] samplers = {new NaiveSampler(), new RefinedSampler(), new GmmtSampler()};
        String[] samplerNames = {"NaiveSampler", "RefinedSampler", "GmmtSampler"};
        for (int i = 0; i < samplers.length; i++) {
            Sampler test = Sampler.getNewSampler(samplerNames[i]);
            Assert.assertEquals(samplers[i].getClass(), test.getClass());
        }
    }
}