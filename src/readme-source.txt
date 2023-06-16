Writer2xhtml source version 1.7.1
=================================

Writer2xhtml is (c) 2002-2023 by Henrik Just.
The source is available under the terms and conditions of the
GNU LESSER GENERAL PUBLIC LICENSE, version 2.1.
Please see the file COPYING.TXT for details.


Overview
--------

The source of Writer2xhtml consists of three major parts:

* A general purpose java library for converting OpenDocument files into LaTeX,
  BibTeX, XHTML, XHTML+MathML, HTML5 and EPUB
  This is to be found in the packages writer2xhtml.* and should only be used
  through the provided api writer2xhtml.api.*
* A command line utility writer2xhtml.Application
* A collection of components for LibreOffice and Apache OpenOffice
  These are to be found in the packages org.openoffice.da.comp.*
  
Currently parts of the source for Writer2LaTeX are somewhat messy and
undocumented. This situation tends to improve over time :-)


Third-party software: JSON.org
------------------------------

The JSON library org.json.* from JSON.org is included in binary form as json-20140107.jar.
The source code is available from JSON.org.

Copyright notice:

The classes org.json.* are copyright (c) 2002 JSON.org and is used subject to the following notice

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
(the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 


Third-party software: BibTeX API
--------------------------------

Villu Ruusmanns Java BibTeX API is included in binary form as jbibtex-1.0.14.jar.
The source code is available from https://code.google.com/p/java-bibtex/

Copyright notice:

Copyright (c) 2012, University of Tartu
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that
the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


Building Writer2LaTeX
---------------------

Writer2LaTeX uses Ant version 1.6 or later (http://ant.apache.org) to build.


Some java libraries from OOo are needed to build the filter part of Writer2LaTeX,
these are jurt.jar, unoil.jar, ridl.jar and juh.jar.

To make these files available for the compiler, edit the file build.xml
as follows:

The lines
	<property name="OFFICE_CLASSES" location="/usr/share/java" />
	<property name="URE_CLASSES" location="/usr/share/java" />
should be modified to the directories where your LO/AOO installation keeps these files
With some LO/AOO installations, you need to install the office development kit as well

The line
	<property name="JAVA6_RT_JAR" location="/home/hj/jre6/lib/rt.jar" />
should be modified to point to rt.jar from a java 6 runtime
(if you want to cross compile for java 6, otherwise change the parameters to the javac task) 

To build, open a command shell, navigate to the source directory and type

ant oxt

(this assumes, that ant is in your path; otherwise specifify the full path.)

In addition to oxt, the build file supports the following targets:
    all
        Build nearly everything
    compile
        Compile all file except the tests.        
    jar
        Create the standalone jar file.
    javadoc
        Create the javadoc documentation in target/javadoc.
    distro
	    Create distribution files 
    clean


Henrik Just, June 2023


Thanks to Michael Niedermair for writing the original ant build file
