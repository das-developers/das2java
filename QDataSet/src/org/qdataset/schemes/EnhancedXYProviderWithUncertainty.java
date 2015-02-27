/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes;

import java.util.Optional;

/**
 *
 * @author jbf
 */
public interface EnhancedXYProviderWithUncertainty extends EnhancedXYData {
    Optional<UncertaintyProvider> getUncertProviderX();
    Optional<UncertaintyProvider> getUncertProviderY();
}
