package no.nav.dokdistsentralprint.itest.config;


import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.nio.file.Path;

import static java.nio.file.Path.of;
import static java.util.Collections.singletonList;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public final class SftpConfig {

	public static SshServer startSshServer(Path tempDir) {
		SshServer sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(0); //random port
		sshServer.setHost("localhost");
		sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(of("hostkey.ser")));
		sshServer.setSubsystemFactories(singletonList(new SftpSubsystemFactory()));
		sshServer.setPublickeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
		sshServer.setUserAuthFactories(singletonList(new UserAuthPublicKeyFactory()));
		sshServer.setFileSystemFactory(new VirtualFileSystemFactory(tempDir));
		try {
			sshServer.start();
			return sshServer;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
