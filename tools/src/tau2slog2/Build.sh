cp -r src/edu/ .
javac -classpath ../jtau_tf/bin/TAU_tf.jar:../contrib/slog2sdk/lib/traceTOslog2.jar edu/uoregon/tau/*
jar -cf tau2slog2.jar edu/
mv tau2slog2.jar bin/
rm -rf edu/
