package de.dfki.lt.mary.unitselection.cart;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

//import com.sun.tools.javac.code.Attribute.Array;

import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.cart.LeafNode.IntAndFloatArrayLeafNode;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class StringPredictionTree extends ExtendedClassificationTree { 
	public static final String ENC_LINE_START = ";;target={";
	public static final String ENC_LINE_END = "}\n";
	
	// TODO: maybe use an HashMap<Integer,String>
	// this strores the strings that correspond to the indices at the leaves
	String[] stringIdDecoding;
	Pattern splitPattern = Pattern.compile("'");
	Pattern delimPattern = Pattern.compile(",\\d+:|}$");
	
    /**
     * 
     * @param rootNode the root node of this tree. This node has to be set to 
     * be a root node beforehand.
     * @param featDef the featureDefinition used in this tree
     * 
     * @author ben
     */
    public StringPredictionTree( Node aRootNode, FeatureDefinition aFeatDef, String[] aTargetDecoding ){
        if (! aRootNode.isRoot())
            throw new IllegalArgumentException("Tried to set a non-root-node as root of the tree. ");
        
        this.rootNode = aRootNode;
        this.featDef = aFeatDef;
        this.stringIdDecoding = aTargetDecoding;
    }

    /**
     * 
     * This constructs a new string prediciton tree from a stream containing 
     * a tree in wagon format.
     * In addition to the constructor of ExtendedClassificationTree it 
     * reads in the mapping from numbers to the Strings from a stream.
     * The encoding has to be the first line in the file (a empty line is allowed).
     * 
     * It has the form:
     * 
     * ;;target={1:'string_a',2:'string_b,'...',26:'string_z'}
     * 
     */

    public StringPredictionTree(BufferedReader reader, FeatureDefinition featDefinition)
            throws IOException {

        String line = reader.readLine(); 

        if ( line.equals("") ){// first line is empty, read again
            line = reader.readLine();
        }
        
        if ( line.startsWith( ENC_LINE_START ) ){
        	
        	// split of the beginning of the string
        	String rawLine = line.substring( (ENC_LINE_START + "0:'").length() );
        	
        	// regular expression for splitting of the target encodings 
        	//									',NUMBER:'  OR   '}
        	
        	String[] splitted = splitPattern.split(rawLine);
        	
        	this.stringIdDecoding = new String[splitted.length / 2];
        	
        	for ( int i = 0 ; i < splitted.length / 2 ; i++ ){
        	    this.stringIdDecoding[i] = splitted[i*2];
        	    if ( ! this.delimPattern.matcher(splitted[i*2 + 1]).matches() ){
        	        throw new IllegalArgumentException("wrong encoding for the mapping of numbers and strings.");
        	    }
        	}
        	
        	//System.err.println(rawLine);
        	//System.err.println(Arrays.toString(stringIdDecoding));
        	
        	// encoding/linebreak problems with this line?
        	//this.stringIdDecoding = rawLine.split("',\\d+:'|'}$");

        	
        } else 
        	throw new IllegalArgumentException("First line must be a comment line specifying the target symbols.");

        // read the rest of the tree
        this.load(reader, featDefinition);
    }
    
	// toString method, that writes the decoding in first line,
	// should be something like:
	// ;;target={1:'string_a',2:'string_b',...,26:'string_z'}
    // this is followed by a 
	public String toString(){
		
		// make String representation of target symbol decoding and invoke super-toString
		StringBuffer sb = new StringBuffer();
		
		sb.append( ENC_LINE_START );
		
		for ( int i = 0 ; i < this.stringIdDecoding.length ; i++ ){
			
			if ( i > 0 )
				sb.append( "," );

			sb.append( i );
			sb.append( ":'" );
			sb.append(this.stringIdDecoding[i]);
			sb.append( "'" );
		}
		
		sb.append( ENC_LINE_END );
		sb.append( super.toString() );
		
		return sb.toString();
	}

    /**
     * TODO: copied from CART, does not work as expected with minNumberOfData = 0
     * 
     * Passes the given item through this CART and returns the
     * leaf Node, or the Node it stopped walking down.
     *
     * @param target the target to analyze
     * @param minNumberOfData the minimum number of data requested.
     * If this is 0, walk down the CART until the leaf level.
     *
     * @return the Node
     */
    public Node interpretToNode(FeatureVector featureVector, int minNumberOfData) {
        Node currentNode = rootNode;
        Node prevNode = null;

        // logger.debug("Starting cart at "+nodeIndex);
        while (currentNode.getNumberOfData() > minNumberOfData
                && !(currentNode instanceof LeafNode)) {
            // while we have not reached the bottom,
            // get the next node based on the features of the target
            prevNode = currentNode;
            currentNode = ((DecisionNode) currentNode)
                    .getNextNode(featureVector);
            // logger.debug(decision.toString() + " result '"+
            // decision.findFeature(item) + "' => "+ nodeIndex);
        }
               
        // Now usually we will have gone down one level too far
        if (currentNode.getNumberOfData() < minNumberOfData
                && prevNode != null) {
            currentNode = prevNode;
        }

        assert currentNode.getNumberOfData() >= minNumberOfData
            || currentNode == rootNode; 
        
        return currentNode;
        
    }
    
    public String getMostProbableString(FeatureVector aFV){

        
        // get the node data
        // TODO: for some reason, when I changed interpretToNode in taking a fv, I had to change mindata to -1 ?!
        IntAndFloatArrayLeafNode predictedNode = (IntAndFloatArrayLeafNode) this.interpretToNode(aFV, -1);
        
        
        // look for the index with highest associated probability
        float[] probs = predictedNode.getFloatData();
        int[] indices = predictedNode.getIntData();
        
        int bestInd = 0;
        float maxProb = 0f;
        
        for ( int i = 0 ; i < indices.length ; i++ ){
            if ( probs[i] > maxProb ){
                maxProb = probs[i];
                bestInd = indices[i];
            }
        }
        
        if ( bestInd >= stringIdDecoding.length ){
            logger.info("looking up most probable string for feature vector");
            logger.error("index bigger than number of targets");
            logger.info("biggest index is " + (stringIdDecoding.length -1 ) + "with the symbol" + stringIdDecoding[stringIdDecoding.length -1] );
        }
        
        // get the String representation
        return this.stringIdDecoding[bestInd];
    }

    	
	public String getMostProbableString(Target aTarget){
		// get the String representation
		return this.getMostProbableString(aTarget.getFeatureVector());
		
	}

}
