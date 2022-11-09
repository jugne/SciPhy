package lineageTree.simulation;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.datatype.DataType;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.JukesCantor;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.BEASTClassLoader;
import beast.util.PackageManager;
import beast.util.Randomizer;
import feast.nexus.CharactersBlock;
import feast.nexus.NexusBuilder;
import feast.nexus.TaxaBlock;
import lineageTree.substitutionmodel.TypewriterSubstitutionModel;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Description("A more flexible alignment simulator adapted from Tim Vaughan's feast implementation")
public class SimulatedAlignment extends Alignment{

        public Input<Tree> treeInput = new Input<>(
                "tree",
                "Tree down which to simulate sequence evolution.",
                Input.Validate.REQUIRED);

        public Input<SiteModel> siteModelInput = new Input<>(
                "siteModel",
                "Site model to use in simulation.",
                Input.Validate.REQUIRED);

        public Input<Integer> sequenceLengthInput = new Input<>(
                "sequenceLength",
                "Length of sequence to simulate.",
                Input.Validate.REQUIRED);

        public Input<Integer> insertionLengthInput = new Input<>(
            "insertionLength",
            "Number of insertions to add per site",
            Input.Validate.REQUIRED);


        public Input<String> outputFileNameInput = new Input<>(
                "outputFileName",
                "Name of file (if any) simulated alignment should be saved to.");

        private Tree tree;
        private SiteModel siteModel;
        private double[] insertionProb;
        private int seqLength;
        private int insertionLength;
        private DataType dataType;

        private String[] ancestralSeqStr;
        private String[] ancestralSequence;

        public SimulatedAlignment() {
            sequenceInput.setRule(Input.Validate.OPTIONAL);
        }

        @Override
        public void initAndValidate() {

            tree = treeInput.get();
            siteModel = siteModelInput.get();
            seqLength = 1; //TODO rewrite for arbitrary #sites! sequenceLengthInput.get();
            insertionLength = insertionLengthInput.get();
            sequences.clear();

            grabDataType();

            simulate();

            super.initAndValidate();

            // Write simulated alignment to disk if required
            if (outputFileNameInput.get() != null) {
                try (PrintStream pstream = new PrintStream(outputFileNameInput.get())) {
                    NexusBuilder nb = new NexusBuilder();
                    nb.append(new TaxaBlock(new TaxonSet(this)));
                    nb.append(new CharactersBlock(this));
                    nb.write(pstream);
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException("Error writing to file "
                            + outputFileNameInput.get() + ".");
                }
            }
        }

        /**
         * Perform actual sequence simulation.
         */
        private void simulate() {
            int nTaxa = tree.getLeafNodeCount();

            TypewriterSubstitutionModel substModel = (TypewriterSubstitutionModel) siteModel.getSubstitutionModel();
            int nStates = substModel.getStateCount();

            double[] transitionProbs = substModel.getInsertionProbs();

            int[][][] alignment = new int[nTaxa][seqLength][insertionLength];

            Node root = tree.getRoot();

            int[][] parentSequence = new int[seqLength][insertionLength];

            ancestralSeqStr = new String[seqLength];

            for (int i=0; i < seqLength; i++){
                //TODO we assume here that state 0 encodes unedited
                ancestralSeqStr[i] = dataType.encodingToString(parentSequence[i]);
            }


            traverse(root, parentSequence,
                    transitionProbs,
                    alignment);

            for (int leafIdx=0; leafIdx<nTaxa; leafIdx++) {
                String seqString = dataType.encodingToString(alignment[leafIdx]);

                String taxonName;
                if (tree.getNode(leafIdx).getID() != null)
                    taxonName = tree.getNode(leafIdx).getID();
                else
                    taxonName = "t" + leafIdx;

                sequenceInput.setValue(new Sequence(taxonName, seqString), this);
            }
        }

        /**
         * Traverse a tree, simulating a sequence alignment down it.
         *
         * @param node Node of the tree
         * @param parentSequence Sequence at the parent node in the tree
         * @param transitionProbs transition probabilities
         * @param regionAlignment alignment for particular region
         */
        private void traverse(Node node,
                              int[][] parentSequence,
                              double[] transitionProbs,
                              int[][][] regionAlignment) {


            // ignore categories so far

            for (Node child : node.getChildren()) {

                // Calculate transition probabilities
            //    for (int i=0; i<siteModel.getCategoryCount(); i++) {
                    //siteModel.getSubstitutionModel().getTransitionProbabilities(
                     //       child, node.getHeight(), child.getHeight(),
                      //      1,
                       //     transitionProbs[0]);
              //  }

                double deltaT = node.getHeight() - child.getHeight();
                double clockRate = siteModel.getRateForCategory(0, child);

                // Draw characters on child sequence
                int[][] childSequence = new int[parentSequence.length][parentSequence[0].length];
                int nStates = dataType.getStateCount();
                double[] charProb = new double[nStates];

                // sample number of new insertions
                long nEdits = Randomizer.nextPoisson(deltaT * clockRate);

                for (int i=0; i<nEdits; i++){

                }
                for (int i=0; i< childSequence.length; i++) {
                    //int category = categories[i];
                    //System.arraycopy(transitionProbs[category],
                    //        parentSequence[i]*nStates, charProb, 0, nStates);
                    //childSequence[i] = Randomizer.randomChoicePDF(charProb);


                }

                if (child.isLeaf()) {
                    System.arraycopy(childSequence, 0,
                            regionAlignment[child.getNr()], 0, childSequence.length);
                } else {
                    traverse(child, childSequence,
                            transitionProbs,
                            regionAlignment);
                }
            }
        }

        /**
         * HORRIBLE function to identify data type from given description.
         */
        private void grabDataType() {
            if (userDataTypeInput.get() != null) {
                dataType = userDataTypeInput.get();
            } else {

                List<String> dataTypeDescList = new ArrayList<>();
                List<String> classNames = PackageManager.find(beast.evolution.datatype.DataType.class, "beast.evolution.datatype");
                for (String className : classNames) {
                    try {
                        DataType thisDataType = (DataType) BEASTClassLoader.forName(className).newInstance();
                        if (dataTypeInput.get().equals(thisDataType.getTypeDescription())) {
                            dataType = thisDataType;
                            break;
                        }
                        dataTypeDescList.add(thisDataType.getTypeDescription());
                    } catch (ClassNotFoundException
                            | InstantiationException
                            | IllegalAccessException e) {
                    }
                }
                if (dataType == null) {
                    throw new IllegalArgumentException("Data type + '"
                            + dataTypeInput.get()
                            + "' cannot be found.  Choose one of "
                            + Arrays.toString(dataTypeDescList.toArray(new String[0])));
                }
            }
        }
}
