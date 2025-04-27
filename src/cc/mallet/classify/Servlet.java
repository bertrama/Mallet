package cc.mallet.classify;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.HashMap;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import cc.mallet.types.Labeling;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Set;
import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Servlet extends HttpServlet {
    private Map<String, Classifier> classifiers;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // We're serving json
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Map<String, String[]> params = request.getParameterMap();

        String data = null;
        if (params.containsKey("data")) {
            data = params.get("data")[0];
        }
        else {
            out.println("No data specified.");
            return;
        }

        String name = null;
        if (params.containsKey("name")) {
            name = params.get("name")[0];
        }

        String target = null;
        if (params.containsKey("target")) {
            target = params.get("target")[0];
        }

        String classifier_name = "default";
        if (params.containsKey("classifier")) {
            classifier_name = params.get("classifier")[0];
        }
        if (!classifiers.containsKey(classifier_name)) {
            out.println("No valid classifier specified.");
            return;
        }

        Classifier classifier = classifiers.get(classifier_name);
        Instance input = new Instance(data, target, name, data);
        ArrayList<Instance> input_array = new ArrayList<Instance>();
        input_array.add(input);
        Iterator<Instance> iterator = classifier.getInstancePipe().newIteratorFrom(input_array.iterator());

        while (iterator.hasNext()) {
            Instance instance = iterator.next();
            Labeling labeling = classifier.classify(instance).getLabeling();

            JSONObject json = new JSONObject();
            JSONArray labels = new JSONArray();

            json.put("data", instance.getData());
            json.put("target", instance.getTarget());
            json.put("name", instance.getName());
            json.put("source", instance.getSource());
            json.put("classifier", classifier_name);

            for ( int location = 0; location < labeling.numLocations(); ++location) {
                JSONObject label = new JSONObject();
                label.put("label", labeling.labelAtLocation(location).toString());
                label.put("value", labeling.valueAtLocation(location));
                labels.put(label);
            }
            json.put("labels", labels);
            out.println(json.toString());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Handle POST requests here
        doGet(request, response);
        return;
    }

    @Override
    public void init() throws ServletException {
        // Initialization code, executed when the servlet is loaded
        String modelsDir = System.getProperty("classifier.models.dir");
        String defaultModel = System.getProperty("classifier.models.default");
        classifiers = new HashMap<String, Classifier>();
        Set<String> models = Stream.of(new File(modelsDir).listFiles())
            .filter(file -> !file.isDirectory())
            .map(File::getName)
            .collect(Collectors.toSet());
        if (defaultModel == null || defaultModel.isEmpty()) {
            defaultModel = (String) models.toArray()[0];
        }

        for (String model : models) {
            try {
                ObjectInputStream ois =
				            new ObjectInputStream(new BufferedInputStream(new FileInputStream(modelsDir + "/" + model)));
                Classifier c = (Classifier) ois.readObject();
                c.getInstancePipe().getDataAlphabet().stopGrowth();
                c.getInstancePipe().getTargetAlphabet().stopGrowth();
                classifiers.put(model, c);
            } catch (Exception e) {
                System.out.println("Unable to load classifiers");
            }
        }
        if (classifiers.containsKey(defaultModel) && ! classifiers.containsKey("default")) {
            classifiers.put("default", classifiers.get(defaultModel));
        }
    }

    @Override
    public void destroy() {
        // Cleanup code, executed when the servlet is unloaded
    }
}
