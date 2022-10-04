
package no.nav.dokdistsentralprint.config.jms;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueue;
import no.nav.dokdistsentralprint.config.alias.MqGatewayAlias;
import no.nav.dokdistsentralprint.config.alias.ServiceuserAlias;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.net.ssl.SSLSocketFactory;

import java.util.concurrent.TimeUnit;

import static com.ibm.mq.constants.CMQC.MQENC_NATIVE;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_CHARACTER_SET;
import static com.ibm.msg.client.jms.JmsConstants.JMS_IBM_ENCODING;
import static com.ibm.msg.client.wmq.common.CommonConstants.WMQ_CM_CLIENT;

@Configuration
@Profile("nais")
public class JmsConfig {

    private static final int UTF_8_WITH_PUA = 1208;
    private static final String ANY_TLS13_OR_HIGHER = "*TLS13ORHIGHER";

    @Bean
    public Queue qdist009(@Value("${dokdistsentralprint_qdist009_dist_s_print.queuename}") String qdist009QueueName) throws JMSException {
        return new MQQueue(qdist009QueueName);
    }

    @Bean
    public Queue qdist009FunksjonellFeil(@Value("${dokdistsentralprint_qdist009_funk_feil.queuename}") String qdist009FunksjonellFeil) throws JMSException {
        return new MQQueue(qdist009FunksjonellFeil);
    }

    @Bean
    public ConnectionFactory wmqConnectionFactory(final MqGatewayAlias mqGatewayAlias,
                                                  final ServiceuserAlias serviceuserAlias) throws JMSException {
        return createConnectionFactory(mqGatewayAlias, serviceuserAlias);
    }

    private PooledConnectionFactory createConnectionFactory(final MqGatewayAlias mqGatewayAlias,
                                                            final ServiceuserAlias serviceuserAlias) throws JMSException {
        MQConnectionFactory connectionFactory = new MQConnectionFactory();
        connectionFactory.setHostName(mqGatewayAlias.getHostname());
        connectionFactory.setPort(mqGatewayAlias.getPort());
        connectionFactory.setQueueManager(mqGatewayAlias.getName());
        connectionFactory.setTransportType(WMQ_CM_CLIENT);
        connectionFactory.setCCSID(UTF_8_WITH_PUA);
        connectionFactory.setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE);
        connectionFactory.setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA);

        // Konfigurasjon for IBM MQ broker med TLS og autorisasjon med serviceuser mot onpremise Active Directory.
        if (mqGatewayAlias.getChannel().isEnabletls()) {
            connectionFactory.setSSLCipherSuite(ANY_TLS13_OR_HIGHER);
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            connectionFactory.setSSLSocketFactory(factory);
            connectionFactory.setChannel(mqGatewayAlias.getChannel().getSecurename());
        } else {
            connectionFactory.setChannel(mqGatewayAlias.getChannel().getName());
        }

        UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
        adapter.setTargetConnectionFactory(connectionFactory);
        adapter.setUsername(serviceuserAlias.getUsername());
        adapter.setPassword(serviceuserAlias.getPassword());

        PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
        pooledFactory.setConnectionFactory(adapter);
        pooledFactory.setMaxConnections(10);
        pooledFactory.setMaximumActiveSessionPerConnection(10);
        pooledFactory.setReconnectOnException(true);
        pooledFactory.setExpiryTimeout(TimeUnit.HOURS.toMillis(24));
        return pooledFactory;
    }
}
