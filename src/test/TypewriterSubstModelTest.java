package test;

import beast.core.Description;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.substitutionmodel.Frequencies;
import lineageTree.substitutionmodel.TypewriterSubstitutionModelHomogeneous;
import org.apache.commons.math.distribution.PoissonDistributionImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;


@Description("Test substitution model")
public class TypewriterSubstModelTest {

    // test that inputs are handled as expected

    TypewriterSubstitutionModelHomogeneous substModel;

    @Before
    public void setUp(){
        substModel = new TypewriterSubstitutionModelHomogeneous();
    }

    //----------------------------------------------------------------------------------//
    // test valid inputs
    @Test (expected = RuntimeException.class)
    public void testExceptionForNegEditFreqInput(){

        RealParameter editFreqs = new RealParameter("-1 2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", editFreqs, "estimate", false);

        // this should fail
        substModel.initByName("editfrequencies", editFreqs, "frequencies", frequencies);
    }

    @Test (expected = RuntimeException.class)
    public void testExceptionForEditFreqSumNot1(){

        RealParameter editFreqs = new RealParameter("0.6 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", editFreqs, "estimate", false);

        // this should fail
        substModel.initByName("editfrequencies", editFreqs, "frequencies", frequencies);
    }

    @Test (expected = RuntimeException.class)
    public void testExceptionForEmptyEditFreqInput(){

        RealParameter editFreqs = new RealParameter();
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", editFreqs, "estimate", false);

        // this should fail
        substModel.initByName("editfrequencies", editFreqs, "frequencies", frequencies);
    }

    //----------------------------------------------------------------------------------//
    // test transition probability calculations
    @Test
    public void testTransitionProbabilities(){


        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "1,2,0,0,0");
        Sequence b = new Sequence("cell2", "1,2,2,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        // expectedProbability : draw 1 event on a Poisson process bounded to 3
        // P(1)*0.2

        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(0.5);
        Double expectedProbability = dist.probability(1) * 0.2 ;

        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);

        // Assert
        assertEquals(expectedProbability, calculatedProbability,0.00001);

    }


    @Test
    public void testTransitionProbabilitiesNoEdit(){

        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "2,1,0,0,0");
        Sequence b = new Sequence("cell2", "2,1,0,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);


        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);

        Double expectedProbability = 0.6065306597126334;

        assertEquals(expectedProbability, calculatedProbability, 1e-10);

    }

    @Test
    public void testTransitionProbabilitiesSingleEdit(){

        // Arrange
        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "2,1,0,0,0");
        Sequence b = new Sequence("cell2", "2,1,1,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);


        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);

        // expectedProbability : draw 1 event on a Poisson process, with event frequency 0.8
        // P(1) * 0.8
        Double expectedProbability = 0.2426122638850534;


        assertEquals(expectedProbability, calculatedProbability, 1e-10);


    }


    @Test
    public void testTransitionProbabilities2Edits(){

        // Arrange
        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "2,1,0,0,0");
        Sequence b = new Sequence("cell2", "2,1,1,2,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);


        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);

        // expectedProbability : draw 2 events on a Poisson process, with event probabilities 0.8 and 0.2 resp
        // P(2) * 0.8 * 0.2
        Double expectedProbability = 0.012130613194252673;

        assertEquals(expectedProbability, calculatedProbability, 1e-10);


    }

    @Test
    public void testTransitionProbabilitiesEditsAndSaturation(){

        // Arrange
        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "2,1,0,0,0");
        Sequence b = new Sequence("cell2", "2,1,1,2,2");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);


        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);

        // expectedProbability : draw 3 events on a Poisson process bounded to 3:
        // with event frequency 0.8, 0.2
        // (1 - P(0) + P(1) + P(2)) * (0.2*0.2*0.8)
        Double expectedProbability = 4.6040569494306294E-4;

        assertEquals(expectedProbability, calculatedProbability, 1e-10);


    }

    @Test
    public void testTransitionProbabilitiesNonsensical(){

        RealParameter freqs = new RealParameter("0.8 0.2");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        substModel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        substModel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "2,0,0,0,0");
        Sequence b = new Sequence("cell2", "1,2,0,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);



        Double calculatedProbability = substModel.getSequenceTransitionProbability( sequence_a, sequence_b,0.5);
        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(0.5);

        // expectedProbability : draw 1 event on a Poisson process bounded to 4:
        // with event frequency 0.8, 0.2
        // P(1) * 0.8

        Double expectedProbability = dist.probability(1)  * 0.8  ;

        assertEquals(expectedProbability, calculatedProbability, 0.00001);
        //THIS IS THE PROBABILITY OF TRANSITION BY ADDING 1, this cannot be!


    }

    @Test
    public void testPoissonDistAgainstR(){
        // This validates that the PoissonDistributionImpl we use to calculate the poisson probabilities
        // behaves as expected. We test this by comparing against the values by Rpois

        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(0.5);
        double p0 = dist.probability(0);
        assertEquals( 0.6065307, p0, 0.00001);
        double p1 = dist.probability(1);
        assertEquals( 0.3032653, p1, 0.00001);
        double p2 = dist.probability(2);
        assertEquals(  0.07581633, p2, 0.00001);
        double p3 = dist.probability(3);
        assertEquals(  0.01263606, p3, 0.00001);
        double p4 = dist.probability(4);
        assertEquals(  0.001579507, p4, 0.00001);

         dist = new PoissonDistributionImpl(0.001);

         p0 = dist.probability(0);
        assertEquals( 0.9990005, p0, 0.00001);
         p1 = dist.probability(1);
        assertEquals( 0.0009990005, p1, 0.00001);
         p2 = dist.probability(2);
        assertEquals(  4.995002e-07, p2, 0.00001);
         p3 = dist.probability(3);
        assertEquals(  1.665001e-10, p3, 0.00001);
         p4 = dist.probability(4);
        assertEquals(  4.162502e-14, p4, 0.00001);

    }


    public void testHighClockNoEdit(){

        // Arrange
        TypewriterSubstitutionModelHomogeneous typewritermodel = new TypewriterSubstitutionModelHomogeneous();

        RealParameter freqs = new RealParameter("1.0");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        typewritermodel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        typewritermodel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "0,0,0,0,0");
        Sequence b = new Sequence("cell2", "0,0,0,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        double clockRate = 40;
        double deltaT = 10;
        double distance = clockRate * deltaT;

        Double calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        Double expectedProbability = 1.91516959671401E-174;

        assertEquals(expectedProbability, calculatedProbability, 1E-16);

    }

    @Test
    public void testAlmost0DistanceWithEdit(){

        //because 0 distance throws already throws an error from the Java implementation part

        // Arrange
        TypewriterSubstitutionModelHomogeneous typewritermodel = new TypewriterSubstitutionModelHomogeneous();

        RealParameter freqs = new RealParameter("1.0");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        typewritermodel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        typewritermodel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "0,0,0,0,0");
        Sequence b = new Sequence("cell2", "1,1,1,0,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        double clockRate = 1E-4;
        double deltaT = 1E-4;
        double distance = clockRate * deltaT;

        Double calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);

        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(distance);

        Double expectedProbability = dist.probability(3);

        System.out.println(expectedProbability);
        System.out.println(calculatedProbability);
        assertEquals(expectedProbability, calculatedProbability, 1E-26);

    }

    @Test
    public void testAlmost0DistanceWithEditSaturated(){

        // Arrange
        TypewriterSubstitutionModelHomogeneous typewritermodel = new TypewriterSubstitutionModelHomogeneous();

        RealParameter freqs = new RealParameter("1.0");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        typewritermodel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        typewritermodel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "0,0,0,0,0");
        Sequence b = new Sequence("cell2", "1,1,1,1,1");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        double clockRate = 0.07;
        double deltaT = 0.1;
        double distance = clockRate * deltaT;

        // calculate for test
        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(distance);
        double expectedProbability = 1;
        int x = 0;
        while (x <= 4){
            expectedProbability -= dist.probability(x);
            x++;
        }

        Double calculatedProbability = typewritermodel.getSequenceTransitionProbability(sequence_a, sequence_b, distance);

        System.out.println(expectedProbability);
        System.out.println(calculatedProbability);

        assertEquals(expectedProbability, calculatedProbability, 1E-16);
    }

    @Test
    public void testDifferentClocksWithEdit(){

        // Arrange
        TypewriterSubstitutionModelHomogeneous typewritermodel = new TypewriterSubstitutionModelHomogeneous();

        RealParameter freqs = new RealParameter("1.0");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        typewritermodel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        typewritermodel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "0,0,0,0,0");
        Sequence b = new Sequence("cell2", "1,1,1,1,0");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        double clockRate = 0.1;
        double deltaT = 10;
        double distance = clockRate * deltaT;

        Double calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(distance );

        Double expectedProbability = dist.probability(4);
        assertEquals(expectedProbability, calculatedProbability, 0.00001);

        clockRate = 0.001;
         deltaT = 10;
         distance = clockRate * deltaT;

         calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        dist = new PoissonDistributionImpl(distance );

         expectedProbability = dist.probability(4);

        assertEquals(expectedProbability, calculatedProbability, 0.00001);

        clockRate = 100;
        deltaT = 10;
        distance = clockRate * deltaT;

        calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        dist = new PoissonDistributionImpl(distance );

        expectedProbability = dist.probability(4);

        assertEquals(expectedProbability, calculatedProbability, 0.00001);

    }

    @Test
    public void testDifferentClocksWithEditSaturation(){

        // Arrange
        TypewriterSubstitutionModelHomogeneous typewritermodel = new TypewriterSubstitutionModelHomogeneous();

        RealParameter freqs = new RealParameter("1.0");
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", freqs, "estimate", false);
        typewritermodel.initByName( "editfrequencies", freqs, "frequencies" ,frequencies);
        typewritermodel.targetBClength = 5;

        Sequence a = new Sequence("cell1", "0,0,0,0,0");
        Sequence b = new Sequence("cell2", "1,1,1,1,1");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", a, "dataType", "integer");
        alignment.initByName("sequence", b, "dataType", "integer");

        //internal representation of the sequences for the package:
        List<Integer> sequence_a = alignment.getCounts().get(0);
        List<Integer> sequence_b = alignment.getCounts().get(1);

        double clockRate = 0.1;
        double deltaT = 10;
        double distance = clockRate * deltaT;

        Double calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        org.apache.commons.math.distribution.PoissonDistribution dist = new PoissonDistributionImpl(distance );

        Double expectedProbability = 1 - (dist.probability(0) + dist.probability(1) + dist.probability(2) +  dist.probability(3) + dist.probability(4));
        assertEquals(expectedProbability, calculatedProbability, 0.00001);

        clockRate = 0.001;
        deltaT = 10;
        distance = clockRate * deltaT;

        calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        dist = new PoissonDistributionImpl(distance );

        expectedProbability = 1 - (dist.probability(0) + dist.probability(1) + dist.probability(2) +  dist.probability(3) + dist.probability(4));


        assertEquals(expectedProbability, calculatedProbability, 0.00001);

        clockRate = 100;
        deltaT = 10;
        distance = clockRate * deltaT;

        calculatedProbability = typewritermodel.getSequenceTransitionProbability( sequence_a, sequence_b, distance);
        dist = new PoissonDistributionImpl(distance );

        expectedProbability = 1 - (dist.probability(0) + dist.probability(1) + dist.probability(2) +  dist.probability(3) + dist.probability(4));


        assertEquals(expectedProbability, calculatedProbability, 0.00001);

    }





}