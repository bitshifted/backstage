/*
 * Copyright (c) 2020. Bitshift (http://bitshifted.co)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package co.bitshifted.xapps.backstage.exception;

/**
 * @author Vladimir Djurovic
 */
public class ContentException extends Exception {

	public ContentException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContentException(Throwable cause) {
		super(cause);
	}
}
