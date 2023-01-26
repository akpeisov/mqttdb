package kz.home.mqttdb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Configuration
@ComponentScan("kz.home.mqttdb")
@PropertySource("classpath:application.properties")
public class SpringConfig {
    private static final Logger logger = LoggerFactory.getLogger(SpringConfig.class);
    // private final ApplicationContext applicationContext;
    private final Environment env;

    @Autowired
    public SpringConfig(ApplicationContext applicationContext, Environment env) {
        // this.applicationContext = applicationContext;
        this.env = env;
    }

    @Bean(destroyMethod = "close")
    public DataSource poolDataSource(){    
        logger.info("Starting create data source...");        
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(env.getRequiredProperty("pool.MaxPoolSize", Integer.class));
        config.setMinimumIdle(env.getRequiredProperty("pool.MinPoolSize", Integer.class));     
        config.setConnectionTimeout(env.getRequiredProperty("pool.ConnectionWaitTimeout", Integer.class));   
        // config.setDriverClassName("oracle.jdbc.pool.OracleDataSource");
        config.setPoolName(env.getRequiredProperty("pool.name"));
                                   
        config.setJdbcUrl(env.getRequiredProperty("pool.URL"));
        config.addDataSourceProperty("user", env.getRequiredProperty("pool.User"));
        config.addDataSourceProperty("password", env.getRequiredProperty("pool.Password"));

        // logger.info(config.getDataSourceProperties().getProperty("user"));            
        // logger.info(config.getDataSourceProperties().getProperty("password"));
        // config.addDataSourceProperty( "cachePrepStmts" , "true" );
        // config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        // config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );

        HikariDataSource ds = new HikariDataSource(config);
        logger.info("Data source created");
        // Connection conn = ds.getConnection();

        return ds;
    }
     
    @Bean
    public JdbcTemplate jdbcTemplate() throws SQLException {                
        return new JdbcTemplate(poolDataSource());
        // return new JdbcTemplate(dataSource());        
    }

    // @Bean
    // public DataSource dataSource() {
    //     logger.warn("DS", "dataSource");
    //     DriverManagerDataSource ds = new DriverManagerDataSource();
    //     ds.setDriverClassName(oracle.jdbc.driver.OracleDriver.class.getName());
    //     ds.setUrl("jdbc:oracle:thin:@tower.home:1521/homepdb");
    //     ds.setUsername("mqtt");
    //     ds.setPassword("mqttPass");
    //     return ds;
    // }
}
