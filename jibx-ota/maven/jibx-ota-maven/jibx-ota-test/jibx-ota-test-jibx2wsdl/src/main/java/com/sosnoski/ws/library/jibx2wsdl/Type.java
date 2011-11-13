/*
 * Copyright 2006 The Apache Software Foundation.
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

package com.sosnoski.ws.library.jibx2wsdl;

public class Type
{
    /** Short name for type. */
    private String m_name;
    
    /** Text description of type. */
    private String m_description;
    
    /** Number of books of this type. */
    private int m_count;
    
    public Type() {}
    
    public Type(String name, String description) {
        m_name = name;
        m_description = description;
    }

    public String getName() {
        return m_name;
    }
    
    public String getDescription() {
        return m_description;
    }
    
    public int getCount() {
        return m_count;
    }
    
    public void setCount(int count) {
        m_count = count;
    }
}