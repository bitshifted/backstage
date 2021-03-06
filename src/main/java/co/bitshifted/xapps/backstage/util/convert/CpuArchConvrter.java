/*
 * Copyright (c) 2020. Bitshift (http://bitshifted.co)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package co.bitshifted.xapps.backstage.util.convert;

import co.bitshifted.xapps.backstage.model.CpuArch;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Vladimir Djurovic
 */
public class CpuArchConvrter implements Converter<String, CpuArch> {
	@Override
	public CpuArch convert(String s) {
		return CpuArch.fromString(s);
	}
}
