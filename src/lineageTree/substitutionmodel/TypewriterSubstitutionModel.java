package lineageTree.substitutionmodel;


import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.datatype.DataType;
import beast.evolution.substitutionmodel.EigenDecomposition;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.util.BEASTClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;


@Description("Specifies transition probability vector for a Typewriter model")
public class TypewriterSubstitutionModel extends SubstitutionModel.Base {
    final public Input<RealParameter> ratesInput = new Input<>("rates",
            "Rates at which each target is cut in the barcode",
            (RealParameter) null);
    /**
     * an m_nStates vector current rates  *
     */
    protected Double[] rateVector;

    /**
     * Used for precalculations, sum of rates
     */
    private double q;
    private boolean updateIntermediates = true;

    private void calculateIntermediates() {
        this.q = arraySum(rateVector);
    }

    /**
     * The sum of the values in the array
     */
    public static double arraySum(Double[] xs) {
        double tmp = 0.0;
        for (double x : xs) {
            tmp += x;
        }
        return(tmp);
    }



    @Override
    public void initAndValidate() {
        super.initAndValidate();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        rateVector = ratesInput.get().getValues();
        Log.info.println("rate vector" + Arrays.asList(rateVector));
        if (rateVector.length != nrOfStates ) {
            throw new IllegalArgumentException("Dimension of input 'rates' is " + rateVector.length + " but a " +
                    "rate vector of dimension " + nrOfStates + " was " +
                    "expected");
        }

    } // initAndValidate



    protected boolean updateMatrix = true;
    private boolean storedUpdateMatrix = true;

    //This is to get transition probabilities for a single editing event
    public double getEditTransitionProbability(int edit, double rate, double startTime, double endTime) {
        if (updateMatrix) {
            calculateIntermediates();
            updateMatrix = false;
        }
        double distance = (startTime - endTime) * rate;
        double pb = (rateVector[edit] - rateVector[edit] * Math.exp(-distance * q)) / q;
        return pb;
    }

    //This is to get transition probability between sequences (with potentially multiple edits)
    public double getSequenceTransitionProbability(List<Integer> sequencea, List<Integer> sequenceb, double distance) {
        List<Integer> subtracted = new ArrayList<>(sequencea);
        subtracted.removeAll(sequenceb);
        double transition_prob = 1;
        for(Integer edit: subtracted) {

            transition_prob = transition_prob * getTransitionProbability(edit, distance);
        }
        //attempt at sequence subtraction
        //need branch length
        //clock rate
        return 0.0;
    }

    public double getTransitionProbability(Integer edit, double distance) {
        if (updateMatrix) {
            calculateIntermediates();
            updateMatrix = false;
        }
        double pb = (rateVector[edit] - rateVector[edit] * Math.exp(-distance * q)) / q;
        return pb;
    }


    /**
     * access to (copy of) rate vector *
     */
    public Double[] getrateVector() {
        return rateVector.clone();
    }



    /**
     * CalculationNode implementation follows *
     */
    @Override
    public void store() {
        storedUpdateMatrix = updateMatrix;
//        System.arraycopy(relativeRates, 0, storedRelativeRates, 0, relativeRates.length);

        super.store();
    }

    /**
     * Restore the additional stored state
     */
    @Override
    public void restore() {

        updateMatrix = storedUpdateMatrix;

        // To restore all this stuff just swap the pointers...
//        double[] tmp1 = storedRelativeRates;
//        storedRelativeRates = relativeRates;
//        relativeRates = tmp1;
        super.restore();

    }

    @Override
    protected boolean requiresRecalculation() {
        // we only get here if something is dirty
        updateMatrix = true;
        return true;
    }


    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {
        double[] full_transition_probabilities = new double[rateVector.length];
        double branchtime = (endTime - startTime) * rate;
        for(int i=1 ; i<=rateVector.length; i++){
            full_transition_probabilities[i] = getTransitionProbability(i,branchtime);
        }
        matrix = full_transition_probabilities;

    }



    @Override
    public EigenDecomposition getEigenDecomposition(Node node) {
        return null;
    }

    /**
     * This function returns the Eigen vectors.
     *
     * @return the array
     */


    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType.getStateCount() != Integer.MAX_VALUE;
    }

} // class GeneralSubstitutionModel
