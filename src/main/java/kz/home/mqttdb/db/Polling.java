package kz.home.mqttdb.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
// import java.sql.CallableStatement;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;
// import java.sql.Types;
// import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

// import kz.home.mqttdb.config.Mqtt;
import kz.home.mqttdb.exceptions.MqttException;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class Polling {
    private static final Logger logger = LoggerFactory.getLogger(Polling.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    // private Connection connection;
    private boolean initialized = false;
    private int id;

    String topics[];
    MqttClient subscribeClient;
    // @Autowired
    // private JdbcTemplate jdbcTemplate;

    @Value("${polling}")
    private boolean polling;

    @Value("${mqtt.url}")
    private String mqtt_url;
    @Value("${mqtt.user}")
    private String mqtt_user;
    @Value("${mqtt.pass}")
    private String mqtt_pass;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Polling(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void refreshTopics() throws SQLException, org.eclipse.paho.client.mqttv3.MqttException {
        String SQL_QUERY = "SELECT topic from mqtt.mqtt_subscribe";        
        try (Connection con = jdbcTemplate.getDataSource().getConnection();
            PreparedStatement pst = con.prepareStatement( SQL_QUERY );
            ResultSet rs = pst.executeQuery();) {
                while ( rs.next() ) {
                    logger.info(rs.getString("topic"));
                    subscribeClient.subscribe(rs.getString("topic"));                    
                }
        }
    }
    
    @PostConstruct // autoexec.bat
    public void connectMqtt() {
        try {
            subscribeClient = new MqttClient(mqtt_url, "mqttDB");
            subscribeClient.setCallback(callback);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(false);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setUserName(mqtt_user);            
            options.setPassword(mqtt_pass.toCharArray());
            subscribeClient.connect(options);                        
            this.refreshTopics();
        } catch (org.eclipse.paho.client.mqttv3.MqttException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }               
    }

    MqttCallback callback = new MqttCallback() {
        public void connectionLost(Throwable t) {
        //   this. connectMqtt();
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {            
            setMessage(topic, new String(message.getPayload()));
        }
      
        public void deliveryComplete(IMqttDeliveryToken token) {
        }                
    };

    @Scheduled(fixedDelayString = "10000")
    public void checkConnection() {
        if (!subscribeClient.isConnected()) {
            this.connectMqtt();
        }
    }
    

    // @PostConstruct // autoexec.bat
    // public void subscribe() throws SQLException, org.eclipse.paho.client.mqttv3.MqttException {
    //     String subscriberId = UUID.randomUUID().toString();
    //     MqttClient subscriber = new MqttClient(mqtt_url, subscriberId);
    //     MqttConnectOptions options = new MqttConnectOptions();
    //     options.setAutomaticReconnect(true);
    //     options.setCleanSession(true);
    //     options.setConnectionTimeout(10);
    //     options.setUserName(mqtt_user);            
    //     options.setPassword(mqtt_pass.toCharArray());
    //     //subscriber.setCallback(callback);
    //     subscriber.connect(options);
        
    //     String SQL_QUERY = "SELECT topic from mqtt.mqtt_subscribe";        
    //     try (Connection con = jdbcTemplate.getDataSource().getConnection();
    //         PreparedStatement pst = con.prepareStatement( SQL_QUERY );
    //         ResultSet rs = pst.executeQuery();) {
    //             while ( rs.next() ) {
    //                 logger.info(rs.getString("topic"));
    //                 subscriber.subscribe(rs.getString("topic"), (s, mqttMessage) -> {
    //                     setMessage(s, new String(mqttMessage.getPayload()));
    //                 });
    //                 // Mqtt.getInstance().subscribeWithResponse(rs.getString("topic"), (s, mqttMessage) -> {
    //                 //             setMessage(s, new String(mqttMessage.getPayload()));
    //                 //             // System.out.println(s);// + mqttMessage.getPayload().toString());
    //                 //             // System.out.println(new String(mqttMessage.getPayload()));            
    //                 //         });
    //             }
    //     }
    // }

    @Scheduled(fixedDelayString = "${pollingTime}")
    private void processPolling() {
        if (!polling)
            return;

        try {
            Connection connection = jdbcTemplate.getDataSource().getConnection();
            CallableStatement callableStatement = connection.prepareCall("{call mqtt.services_pkg.getqueue(?, ?, ?, ?)}");
            callableStatement.registerOutParameter(1, Types.NUMERIC);
            callableStatement.registerOutParameter(2, Types.VARCHAR);
            callableStatement.registerOutParameter(3, Types.VARCHAR);
            callableStatement.registerOutParameter(4, Types.NUMERIC);
                        
            while (true) {
                callableStatement.execute();  
                id = callableStatement.getInt(1);
                String message = callableStatement.getString(2);
                String topic = callableStatement.getString(3);
                int qos = callableStatement.getInt(4);
                if (id > 0) {
                    // something exists                                    
                    String result = publish(message, topic, qos, false);                 
                    setPollingResult(id, result);
                } else {
                    break;
                }          
            }
            callableStatement.close();
            connection.close();
        } catch (SQLException ee) {
            // dbservice.setErrorText("sqlState " + ee.getSQLState() + ". sqlMessage " + ee.getMessage());
            // dbservice.setErrorCode(ee.getErrorCode());
            ee.printStackTrace();
            // connection.close();                                                                
        } finally {
            
        }
    }

    String publish(String message, String topic, int qos, boolean retained) {
        String res = "OK";
        logger.info("Publish topic {} message {} qos {}", topic, message, qos);                    
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(retained);
        try {
            subscribeClient.publish(topic, mqttMessage);
        } catch (MqttPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            res = e.getLocalizedMessage();
        } catch (org.eclipse.paho.client.mqttv3.MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            res = e.getLocalizedMessage();
        }
        //Mqtt.getInstance().publish(topic, mqttMessage);
        return res;
    }

    void setMessage(String topic, String message) {
        try {
            Connection connection = jdbcTemplate.getDataSource().getConnection();
            CallableStatement callableStatement = connection.prepareCall("{call mqtt.services_pkg.setMessage(?, ?)}");
            callableStatement.setString(1, topic);
            callableStatement.setString(2, message);
            callableStatement.execute();                          
            callableStatement.close();        
            connection.close();    
        } catch (SQLException ee) {
            ee.printStackTrace();            
        }
    }

    void setPollingResult(int id, String message) {
        try {
            Connection connection = jdbcTemplate.getDataSource().getConnection();
            CallableStatement callableStatement = connection.prepareCall("{call mqtt.services_pkg.setQueueResult(?, ?)}");
            callableStatement.setInt(1, id);
            callableStatement.setString(2, message);
            callableStatement.execute();                          
            callableStatement.close();   
            connection.close();         
        } catch (SQLException ee) {
            ee.printStackTrace();            
        }
    }
}
