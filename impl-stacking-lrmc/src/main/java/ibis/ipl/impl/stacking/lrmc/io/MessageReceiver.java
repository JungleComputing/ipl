/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.impl.stacking.lrmc.io;

import ibis.ipl.impl.stacking.lrmc.util.Message;

//import ibis.ipl.IbisIdentifier;

public interface MessageReceiver {
    // Returns false if the ObjectMulticaster is done().
    public boolean gotMessage(Message buffer);

    // Called at sender site when the last receiver received last message.
    public void gotDone(int id);
}
