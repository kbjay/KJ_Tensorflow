/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package gdut.bsx.tensorflowtraining.ternsorflow;

import android.graphics.Bitmap;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier implements Classifier {
    private static final String TAG = "TensorFlowImageClassifier";
    // Config values.
    private String inputName;
    private String outputName;
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private float[] outputScore;
    private String[] outputNames;
    private String[] tags;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;
    private TensorFlowInferenceInterface inferenceInterfaceScore;

    private TensorFlowImageClassifier() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labels        The filepath of label file for classes.
     * @param inputSize     The input size. A square image of inputSize x inputSize is assumed.
     * @param inputName     The label of the image input node.
     * @param outputName    The label of the output node.
     * @throws IOException
     */
    public static Classifier create(
            String modelPath,
            String modelFilename,
            String modelScoreFileName,
            String[] labels,
            int inputSize,
            String inputName,
            String outputName) {
        TensorFlowImageClassifier c = new TensorFlowImageClassifier();
        c.inputName = inputName;
        c.outputName = outputName;
        c.tags = labels;
        String path = modelPath + modelFilename;
        String pathScore = modelPath + modelScoreFileName;
        try {
            c.inferenceInterface = new TensorFlowInferenceInterface(new FileInputStream(new File(path)));
            c.inferenceInterfaceScore = new TensorFlowInferenceInterface(new FileInputStream(new File(pathScore)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        c.inputSize = inputSize;
        c.outputNames = new String[]{outputName};
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];
        c.outputs = new float[labels.length];
        c.outputScore= new float[2];
        return c;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF)) / 128f - 1.0f;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF)) / 128f - 1.0f;
            floatValues[i * 3 + 2] = ((val & 0xFF)) / 128f - 1.0f;
        }
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);
        inferenceInterface.run(outputNames, logStats);
        inferenceInterface.fetch(outputName, outputs);
        int index = 0;
        float maxPercent = 0f;
        for (int i = 0; i < outputs.length; i++) {
            if (maxPercent < outputs[i]) {
                maxPercent = outputs[i];
                index = i;
            }
        }
        recognitions.add(new Recognition( "LABEL", tags[index], maxPercent, null));

        inferenceInterfaceScore.feed(inputName,floatValues,1,inputSize,inputSize,3);
        inferenceInterfaceScore.run(outputNames,logStats);
        inferenceInterfaceScore.fetch(outputName,outputScore);
        recognitions.add(new Recognition("SCORE","GOOD="+outputScore[0]+" POOR="+outputScore[1],null,null));

        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean logStats) {
        this.logStats = logStats;
    }

    @Override
    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }
}