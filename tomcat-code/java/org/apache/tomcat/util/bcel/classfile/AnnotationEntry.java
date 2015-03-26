/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tomcat.util.bcel.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.bcel.Constants;

/**
 * represents one annotation in the annotation table
 *
 * @author  <A HREF="mailto:dbrosius@mebigfatguy.com">D. Brosius</A>
 * @since 6.0
 */
public class AnnotationEntry implements Constants {

    private final int type_index;
    private final ConstantPool constant_pool;

    private final List<ElementValuePair> element_value_pairs;

    /**
     * Creates an AnnotationEntry from a DataInputStream
     *
     * @param file
     * @param constant_pool
     * @throws IOException
     */
    AnnotationEntry(DataInput file, ConstantPool constant_pool) throws IOException {

        this.constant_pool = constant_pool;

        type_index = file.readUnsignedShort();
        int num_element_value_pairs = file.readUnsignedShort();

        element_value_pairs = new ArrayList<>(num_element_value_pairs);
        for (int i = 0; i < num_element_value_pairs; i++) {
            element_value_pairs.add(new ElementValuePair(file, constant_pool));
        }
    }

    /**
     * @return the annotation type name
     */
    public String getAnnotationType() {
        final ConstantUtf8 c = (ConstantUtf8) constant_pool.getConstant(type_index, CONSTANT_Utf8);
        return c.getBytes();
    }

    /**
     * @return the element value pairs in this annotation entry
     */
    public List<ElementValuePair> getElementValuePairs() {
        return element_value_pairs;
    }
}
