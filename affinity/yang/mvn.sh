mvn archetype:generate -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
     -DarchetypeGroupId=org.opendaylight.controller.archetypes \
     -DarchetypeArtifactId=odl-model-project \
     -DarchetypeVersion=1.0-SNAPSHOT \
     -DinteractiveMode=false \
     -DgroupId=org.opendaylight.controller.affinity \
     -DartifactId=api \
     -Dversion=0.4.1-SNAPSHOT

