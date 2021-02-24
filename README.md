## Ichos Audio Library

### What does this do?
Expands on the data structures provided by the Almost Realism Scientific
Computing Libraries to provide support for common audio operations such as
FFT.

### What does it depend on?
The dependency footprint is very small. The only dependency is ar-utils
(which depends only on JOCL).

### Who are the authors?
This code was written by Michael Murray, and some of it (MFCC Feature
Extraction) was inspired by detailed reading of code in Kaldi ASR (which
has the same LICENSE).

### To use the libraries

Add Maven Repository:

        <repositories>
                <repository>
                        <id>internal</id>
                        <name>Archiva Managed Internal Repository</name>
                        <url>http://mvn.almostrealism.org:8080/repository/internal/</url>
                        <releases><enabled>true</enabled></releases>
                        <snapshots><enabled>true</enabled></snapshots>
                </repository>
        </repositories>

Add audio:

        <dependency>
            <groupId>org.almostrealism</groupId>
            <artifactId>ar-audio</artifactId>
            <version>1.0</version>
        </dependency>


### What are the terms of the license?

Copyright 2021  Michael Murray

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
