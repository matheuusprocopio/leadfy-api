package com.leadfy.api.repository.projection;

import com.leadfy.api.enums.LeadStatus;

public interface LeadStatusCountProjection {

	LeadStatus getStatus();

	Long getTotal();
}
