package com.leadfy.api.repository.projection;

import com.leadfy.api.enums.LeadSource;

public interface LeadSourceConversionProjection {

	LeadSource getSource();

	Long getTotal();

	Long getClosedLeads();
}
