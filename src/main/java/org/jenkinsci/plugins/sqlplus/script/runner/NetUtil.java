package org.jenkinsci.plugins.sqlplus.script.runner;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetUtil {


	public static String getHostName() {

		try {
			InetAddress addr = InetAddress.getLocalHost();

			String ipAddress = addr.getHostAddress();

			String fullyQualifiedDomainName = addr.getCanonicalHostName();

			if (fullyQualifiedDomainName == null) {
				return ipAddress;
			}

			return fullyQualifiedDomainName;

		} catch (UnknownHostException e) {
			return "localhost";
		}
	}

}
