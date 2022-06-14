/*
 *
 *  * Copyright (c) 2022  Bitshift D.O.O (http://bitshifted.co)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package co.bitshifted.backstage.dto

data class RequiredResourcesDTO(
    var dependencies : MutableList<JvmDependencyDTO> = mutableListOf(),
    var resources : MutableList<BasicResourceDTO> = arrayListOf()
) {
    fun addDependency(dependency : JvmDependencyDTO) = dependencies.add(dependency)

    fun addResource(resource : BasicResourceDTO) = resources.add(resource)
}
