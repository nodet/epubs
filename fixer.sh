M2_REPO=${HOME}/.m2/repository
java -cp ${M2_REPO}/fr/gnodet/epubs/fixer/1.0-SNAPSHOT/fixer-1.0-SNAPSHOT.jar:${M2_REPO}/fr/gnodet/epubs/core/1.0-SNAPSHOT/core-1.0-SNAPSHOT.jar:${M2_REPO}/com/googlecode/juniversalchardet/juniversalchardet/1.0.3/juniversalchardet-1.0.3.jar:${M2_REPO}/net/arnx/jsonic/1.3.10/jsonic-1.3.10.jar::${M2_REPO}/net/sf/jtidy/jtidy/r938/jtidy-r938.jar:${M2_REPO}/org/idpf/epubcheck/4.0.1/epubcheck-4.0.1.jar:${M2_REPO}/com/google/guava/guava/23.0/guava-23.0.jar:${M2_REPO}/org/daisy/libs/jing/20120724.0.0/jing-20120724.0.0.jar:${M2_REPO}/net/sf/saxon/Saxon-HE/9.5.1-5/Saxon-HE-9.5.1-5.jar:${M2_REPO}/org/codehaus/jackson/jackson-core-asl/1.9.13/jackson-core-asl-1.9.13.jar:${M2_REPO}/org/codehaus/jackson/jackson-mapper-asl/1.9.13/jackson-mapper-asl-1.9.13.jar fr.gnodet.epubs.fixer.Main $*
