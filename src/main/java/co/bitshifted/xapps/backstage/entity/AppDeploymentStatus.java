/*
 * Copyright (c) 2020. Bitshift (http://bitshifted.co)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package co.bitshifted.xapps.backstage.entity;

import co.bitshifted.xapps.backstage.dto.DeploymentStatusDTO;
import co.bitshifted.xapps.backstage.model.DeploymentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import javax.persistence.*;
import java.time.ZonedDateTime;

/**
 * @author Vladimir Djurovic
 */
@Entity(name = "app_deployment_status")
@Data
@NoArgsConstructor
public class AppDeploymentStatus {

	@Transient
	private final ModelMapper mapper = new ModelMapper();

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "app_id")
	private Application application;
	private ZonedDateTime submittedAt;

	@Enumerated(EnumType.STRING)
	private DeploymentStatus currentStatus;
	private String details;

	public  AppDeploymentStatus(String id, Application application) {
		this.id = id;
		this.application = application;
		this.currentStatus = DeploymentStatus.ACCEPTED;
	}

	public DeploymentStatusDTO convertToDto() {
		return mapper.map(this, DeploymentStatusDTO.class) ;
	}
}
