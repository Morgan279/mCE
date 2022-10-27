package edu.ecnu.aidadblab.importer;

import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.constant.LocationComponent;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.List;

public class YelpImporter {
    public static String INPUT_DIR = GlobalConfig.getDatasetDir() + "/yelp/";

    private static final String DATA_PREFIX = "yelp_academic_dataset_";

    private final JsonTxtImporter jsonTxtImporter = new JsonTxtImporter(INPUT_DIR);

    private Graph dataGraph;

    private List<JSONObject> data;

    public void loadDataGraph(Graph dataGraph) {
        this.dataGraph = dataGraph;
        this.readBusiness();
        this.readUser();
        this.readReview();
    }

    public List<JSONObject> loadYelp() {
        this.data = this.jsonTxtImporter.readLine(DATA_PREFIX + "business.json");
        return data;
    }

    private void readBusiness() {
        if (this.data == null) {
            loadYelp();
        }
        for (JSONObject dataItem : data) {
            // dataset BUG: some of the same ids will be given as business_id and some as user_id
            Vertex business = new Vertex(dataItem.getString("business_id") + LabelConst.ENTITY_LABEL, LabelConst.ENTITY_LABEL);
            Vertex name = new Vertex(dataItem.getString("name"));
            Vertex star = new Vertex(dataItem.getString("stars"));
            Vertex city = new Vertex(dataItem.getString("city"));
            Vertex isOpen = new Vertex(dataItem.getString("is_open"));
            Vertex state = new Vertex(dataItem.getString("state"));
            Vertex postalCode = new Vertex(dataItem.getString("postal_code"));
            Vertex address = new Vertex(dataItem.getString("address"));
            Vertex reviewCount = new Vertex(dataItem.getString("review_count"));

            double latitude = dataItem.getDoubleValue(LocationComponent.LATITUDE);
            double longitude = dataItem.getDoubleValue(LocationComponent.LONGITUDE);
            JSONObject location = new JSONObject();
            location.put(LocationComponent.LATITUDE, latitude);
            location.put(LocationComponent.LONGITUDE, longitude);
            Vertex locationVertex = new Vertex(location.toJSONString(), LabelConst.LOCATION_LABEL);

            dataGraph.addVertex(business);
            dataGraph.addVertex(locationVertex);
            dataGraph.addVertex(name);
            dataGraph.addVertex(state);
            dataGraph.addVertex(postalCode);
            dataGraph.addVertex(address);
            dataGraph.addVertex(star);
            dataGraph.addVertex(city);
            dataGraph.addVertex(isOpen);
            dataGraph.addVertex(reviewCount);
            dataGraph.addEdge(business, name);
            dataGraph.addEdge(business, state);
            dataGraph.addEdge(business, postalCode);
            dataGraph.addEdge(business, address);
            dataGraph.addEdge(business, star);
            dataGraph.addEdge(business, city);
            dataGraph.addEdge(business, isOpen);
            dataGraph.addEdge(business, reviewCount);
            dataGraph.addEdge(business, locationVertex);

            if (dataItem.getString("categories") != null) {
                for (String categoryString : dataItem.getString("categories").split(", ")) {
                    Vertex category = new Vertex(categoryString);
                    dataGraph.addVertex(category);
                    dataGraph.addEdge(business, category);
                }
            }

            if (dataItem.getJSONObject("hours") != null) {
                JSONObject hours = dataItem.getJSONObject("hours");
                for (String hour : hours.keySet()) {
                    Vertex hourVertex = new Vertex(hour + "-" + hours.getString(hour));
                    dataGraph.addVertex(hourVertex);
                    dataGraph.addEdge(business, hourVertex);
                }
            }

        }
    }

    public void readReview() {
        List<JSONObject> data = this.jsonTxtImporter.readLine(DATA_PREFIX + "review.json");
        for (JSONObject dataItem : data) {
            Vertex user = new Vertex(dataItem.getString("user_id"), "user");
            Vertex text = new Vertex(dataItem.getString("text"), "review_text");
            Vertex reviewStar = new Vertex(dataItem.getString("stars"));
            Vertex reviewDate = new Vertex(dataItem.getString("date"));
            Vertex business = new Vertex(dataItem.getString("business_id"), LabelConst.ENTITY_LABEL);
            Vertex review = new Vertex(dataItem.getString("review_id"), "review");
            if (!dataGraph.hasVertex(user)) { dataGraph.addVertex(user); }
            if (!dataGraph.hasVertex(business)) {
                dataGraph.addVertex(business);
            }

            dataGraph.addVertex(review);
            dataGraph.addVertex(text);
            dataGraph.addVertex(reviewStar);
            dataGraph.addVertex(reviewDate);

            dataGraph.addEdge(review, text);
            dataGraph.addEdge(review, reviewStar);
            dataGraph.addEdge(review, reviewDate);
            dataGraph.addEdge(review, user);
            dataGraph.addEdge(business, review);
        }
    }

    private void readUser() {
        List<JSONObject> data = this.jsonTxtImporter.readLine(DATA_PREFIX + "user.json");
        for (JSONObject dataItem : data) {
            Vertex user = new Vertex(dataItem.getString("user_id"), "user");
            Vertex name = new Vertex(dataItem.getString("name"));
            Vertex reviewCount = new Vertex(dataItem.getString("review_count"));
            if (!dataGraph.hasVertex(user)) {
                dataGraph.addVertex(user);
            }
            dataGraph.addVertex(name);
            dataGraph.addVertex(reviewCount);
            dataGraph.addEdge(user, name);
            dataGraph.addEdge(user, reviewCount);

            for (String friendId : dataItem.getString("friends").split(", ")) {
                Vertex friend = new Vertex(friendId, "user");
                if (!dataGraph.hasVertex(friend)) {
                    dataGraph.addVertex(friend);
                }
                dataGraph.addEdge(user, friend);
            }
        }
    }

}
