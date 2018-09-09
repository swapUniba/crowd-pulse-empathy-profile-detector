package com.github.swapUniba.pulse.crowd.empathy;

import com.github.frapontillo.pulse.crowd.data.entity.Empathy;
import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.crowd.data.entity.Profile;
import com.github.frapontillo.pulse.crowd.data.repository.ProfileRepository;
import com.github.frapontillo.pulse.spi.IPlugin;
import com.github.frapontillo.pulse.spi.IPluginConfig;
import com.github.frapontillo.pulse.spi.PluginConfigHelper;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.Logger;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SafeSubscriber;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import static com.github.frapontillo.pulse.util.PulseLogger.getLogger;

/**
 * EmpathyProfileDetector plugin class.
 *
 * @author Cosimo Lovascio
 *
 */
public class EmpathyProfileDetector extends IPlugin<Message, Message, EmpathyProfileDetector.EmpathyProfileDetectorConfig> {

    private static final String PLUGIN_NAME = "empathy-profile-detector";
    private final static Logger logger = getLogger(EmpathyProfileDetector.class);

    private ProfileRepository profileRepository;

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public EmpathyProfileDetectorConfig getNewParameter() {
        return new EmpathyProfileDetectorConfig();
    }

    @Override
    protected Operator<Message, Message> getOperator(EmpathyProfileDetectorConfig params) {

        // access to profiles database
        profileRepository = new ProfileRepository(params.getProfilesDatabaseName());

        return subscriber -> new SafeSubscriber<>(new Subscriber<Message>() {

            List<Message> allMessages = new ArrayList<>();

            @Override
            public void onCompleted() {
                Profile user = profileRepository.getByUsername(params.getUsername());
                if (user != null) {
                    List<Empathy> userEmpathyList = user.getEmpathies();
                    if (userEmpathyList == null) {
                        userEmpathyList = new ArrayList<>();
                    }
                    userEmpathyList.add(calculateEmpathy(allMessages));
                    logger.info("Empathy calculated");

                    // update the user profile
                    Query<Profile> query = profileRepository.createQuery();
                    query.field("username").equal(user.getUsername());
                    UpdateOperations<Profile> update = profileRepository.createUpdateOperations();
                    update.set("empathies", userEmpathyList);
                    profileRepository.updateFirst(query, update);

                } else {
                    logger.info("No user profile found");
                }

                reportPluginAsCompleted();
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                reportPluginAsErrored();
                subscriber.onError(e);
            }

            @Override
            public void onNext(Message message) {
                allMessages.add(message);
                subscriber.onNext(message);
            }
        });
    }

    /**
     * Calculate empathy based on all messages.
     * @param messages the messages
     * @return the user empathy
     */
    private Empathy calculateEmpathy(List<Message> messages) {

        long timestamp = Calendar.getInstance().getTimeInMillis();

        Empathy userEmpathy = new Empathy();
        userEmpathy.setTimestamp(timestamp);

        String query = "http://90.147.170.25:8080/PersonalityEmpathy/rest/UserService/userEmpathy";
        ArrayList<String> list = new ArrayList<String>();

        for (Message message : messages)
        {
            list.add(message.getText());
        }
        JSONObject json = new JSONObject();
        json.put("messages", new JSONArray(list));

        URL url = new URL(query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");

        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes("UTF-8"));
        os.close();

        // read the response
        InputStream in = new BufferedInputStream(conn.getInputStream());
        String result = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

        JSONObject jsonObject = new JSONObject(result);
        String empathy = jsonObject.getString("empathy");

        /***/
        userEmpathy.setValue(Double.parseDouble(empathy));
        userEmpathy.setSource("empathy-detector");
        userEmpathy.setConfidence(Double.parseDouble(empathy));

        return userEmpathy;
    }

    /**
     * EmpathyProfileDetector configuration class.
     */
    public class EmpathyProfileDetectorConfig implements IPluginConfig<EmpathyProfileDetectorConfig> {

        private String profilesDatabaseName;
        private String username;

        @Override public EmpathyProfileDetectorConfig buildFromJsonElement(JsonElement json) {
            return PluginConfigHelper.buildFromJson(json, EmpathyProfileDetectorConfig.class);
        }

        public String getProfilesDatabaseName() {
            return profilesDatabaseName;
        }

        public void setProfilesDatabaseName(String profilesDatabaseName) {
            this.profilesDatabaseName = profilesDatabaseName;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

    }

}