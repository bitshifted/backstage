/*
 * Copyright (c) 2020. Bitshift (http://bitshifted.co)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package co.bitshifted.xapps.backstage.content;

import co.bitshifted.xapps.backstage.model.*;

import java.net.URI;

/**
 * Defines methods for retrieving various data mappings.
 */
public interface ContentMapping {

	URI getWorkspaceUri();

	URI getJdkStorageUri();

	URI getLauncherStorageUri();

	URI getSyncroStorageUri();

	URI getUpdatesDownloadLocation();

	URI getJdkLocation(JdkProvider provider, JvmImplementation jvmImplementation, JdkVersion version, OS os, CpuArch cpuArch);

	URI getUpdatesParentLocation(String applicationId, String releaseNumber, OS os, CpuArch cpuArch);

	URI getUpdateFile(String fileName, String applicationId, String releaseNumber, OS os, CpuArch cpuArch);

}
