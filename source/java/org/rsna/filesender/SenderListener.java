/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.filesender;

import java.util.EventListener;

/**
 * The interface for listeners to SenderEvents.
 */
public interface SenderListener extends EventListener {

	public void fileSent(SenderEvent event);

}
