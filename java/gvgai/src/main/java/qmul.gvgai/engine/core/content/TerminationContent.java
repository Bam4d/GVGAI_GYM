package qmul.gvgai.engine.core.content;

import java.util.HashMap;


public class TerminationContent extends Content {

    /**
     * Constructor that extracts the contents from a String line
     *
     * @param line String with the contents in VGDL format, to be mapped to the
     *             data structures of this class.
     * @throws Exception
     */
    public TerminationContent(String line) throws Exception {
        this.line = line;

        //Init structures of node content.
        parameters = new HashMap<String, String>();

        //Take the pieces and the first one is the name that defines the content
        String pieces[] = line.split(" ");
        identifier = pieces[0].trim();

        if (pieces.length < 2) {
            //This is the TerminationSet line. Just finish here
            return;
        }

        //Take the other pieces and extract properties and parameters key-value.
        for (int i = 1; i < pieces.length; ++i) {
            String piece = pieces[i].trim();
            if (piece.contains("=")) {
                String keyValue[] = piece.split("=");
                if (keyValue.length > 1) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    parameters.put(key, value);
                } else {
                    throw new Exception(keyValue[0] + " has no value.");
                }
            }
        }
    }


    @Override
    public void decorate(HashMap<String, ParameterContent> pcs) {
        _decorate(pcs);
    }
}