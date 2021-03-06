package dk.dma.aisbus;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import dk.frv.ais.reader.RoundRobinAisTcpReader;

public class Settings {

	private static final Logger LOG = Logger.getLogger(Settings.class);

	private Properties props;
	private int doubleFilterWindow;
	private int downsamplingRate;

	private MessageBus messageBus;
	private Map<String, TcpReader> tcpReaders = new HashMap<String, TcpReader>();
	private Map<String, TcpServer> tcpServers = new HashMap<String, TcpServer>();
	private Map<String, TcpWriter> tcpWriters = new HashMap<String, TcpWriter>();
	private Map<String, TcpProvider> tcpProviders = new HashMap<String, TcpProvider>();

	public Settings() {

	}

	public void load(String filename) throws IOException {
		props = new Properties();
		URL url = ClassLoader.getSystemResource(filename);
		if (url == null) {
			throw new IOException("Could not find properties file: " + filename);
		}
		props.load(url.openStream());

		// Determine doublet filtering
		doubleFilterWindow = getInt("doublet_filtering", "10");

		// Determine downsampling
		downsamplingRate = getInt("downsampling", "0");

		messageBus = new MessageBus(doubleFilterWindow, downsamplingRate);

		// Create TCP readers
		String tcpReadersStr = props.getProperty("tcp_readers", "");
		for (String name : StringUtils.split(tcpReadersStr, ",")) {
			RoundRobinAisTcpReader reader = new RoundRobinAisTcpReader();
			reader.setName(name);
			String hostsStr = props.getProperty("tcp_reader_hosts." + name, "");
			reader.setCommaseparatedHostPort(hostsStr);
			if (reader.getHostCount() == 0) {
				LOG.error("No valid TCP source hosts given");
				System.exit(1);
			}
			reader.setTimeout(getInt("tcp_reader_timeout." + name, "10"));
			reader.setReconnectInterval(getInt("tcp_reader_reconnect_interval." + name, "5") * 1000);

			// Make TCP reader
			TcpReader tcpReader = new TcpReader(reader, messageBus);
			tcpReader.setDownsamplingRate(getInt("tcp_reader_downsampling." + name, "0"));
			tcpReader.setDoubleFilterWindow(getInt("tcp_reader_doublet_filtering." + name, "0"));
			tcpReader.setMessageFilter(props.getProperty("tcp_reader_message_id_filter." + name, ""));
			tcpReader.setGzipCompress(props.getProperty("tcp_reader_gzip_compress." + name, "").equalsIgnoreCase("true"));
			tcpReader.setSourceName(props.getProperty("tcp_reader_source_name." + name, null));

			tcpReaders.put(name, tcpReader);

			LOG.info("Added TCP reader " + name + " (" + hostsStr + ")");
		}

		// Create TCP servers
		String tcpServersStr = props.getProperty("tcp_servers", "");
		for (String name : StringUtils.split(tcpServersStr, ",")) {
			TcpServer tcpServer = new TcpServer(messageBus);
			tcpServer.setPort(getInt("tcp_server_port." + name, "0"));
			tcpServer.setTimeout(getInt("tcp_server_timeout." + name, "0"));
			tcpServer.setDownsamplingRate(getInt("tcp_server_downsampling." + name, "0"));
			tcpServer.setDoubleFilterWindow(getInt("tcp_server_doublet_filtering." + name, "0"));
			tcpServer.setMessageFilter(props.getProperty("tcp_server_message_id_filter." + name, ""));
			tcpServer.setGzipCompress(props.getProperty("tcp_server_gzip_compress." + name, "").equalsIgnoreCase("true"));
			tcpServer.setSourceName(props.getProperty("tcp_server_source_name." + name, null));

			tcpServers.put(name, tcpServer);

			LOG.info("Added TCP server " + name + " (" + tcpServer.getPort() + ")");
		}

		// Create TCP servers
		String tcpWritersStr = props.getProperty("tcp_writers", "");
		for (String name : StringUtils.split(tcpWritersStr, ",")) {
			TcpWriter tcpWriter = new TcpWriter(messageBus);
			tcpWriter.setHost(props.getProperty("tcp_writer_host." + name, ""));
			tcpWriter.setPort(getInt("tcp_writer_port." + name, "0"));
			tcpWriter.setDownsamplingRate(getInt("tcp_writer_downsampling." + name, "0"));
			tcpWriter.setDoubleFilterWindow(getInt("tcp_writer_doublet_filtering." + name, "0"));
			tcpWriter.setMessageFilter(props.getProperty("tcp_writer_message_id_filter." + name, ""));
			tcpWriter.setGzipCompress(props.getProperty("tcp_writer_gzip_compress." + name, "").equalsIgnoreCase("true"));
			
			tcpWriters.put(name, tcpWriter);
			
			LOG.info("Added TCP writer " + name + " (" + tcpWriter.getHost() + ":" + tcpWriter.getPort() + ")");
		}
		
		// Create TCP providers
		String tcpProvidersStr = props.getProperty("tcp_providers", "");
		for (String name : StringUtils.split(tcpProvidersStr, ",")) {
			TcpProvider tcpProvider = new TcpProvider(messageBus);
			tcpProvider.setPort(getInt("tcp_provider_port." + name, "0"));
			tcpProvider.setDownsamplingRate(getInt("tcp_provider_downsampling." + name, "0"));
			tcpProvider.setDoubleFilterWindow(getInt("tcp_provider_doublet_filtering." + name, "0"));
			tcpProvider.setMessageFilter(props.getProperty("tcp_provider_message_id_filter." + name, ""));
			tcpProvider.setGzipCompress(props.getProperty("tcp_provider_gzip_compress." + name, "").equalsIgnoreCase("true"));
			
			tcpProviders.put(name, tcpProvider);
			
			LOG.info("Added TCP provider " + name + " (" + tcpProvider.getPort() + ")");
		}

	}

	private int getInt(String key, String defaultValue) {
		String val = props.getProperty(key, defaultValue);
		return Integer.parseInt(val);
	}

	public Map<String, TcpReader> getTcpReaders() {
		return tcpReaders;
	}

	public Map<String, TcpWriter> getTcpWriters() {
		return tcpWriters;
	}

	public Map<String, TcpServer> getTcpServers() {
		return tcpServers;
	}
	
	public Map<String, TcpProvider> getTcpProviders() {
		return tcpProviders;
	}

	public int getDoubleFilterWindow() {
		return doubleFilterWindow;
	}

	public int getDownsamplingRate() {
		return downsamplingRate;
	}

}
