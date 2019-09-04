package com.mineraltree.releng

/**
 * Represents a java application "entry point". An EntryPoint of this type represents a public "main"
 * function runnable as a process. The application plugin can generate a start shell script which
 * will launch a process using this as its main routine.
 */
class EntryPoint {

  /**
   * The name of the entry point. This name is used as the start script file name
   */
  final String name

  /**
   * The fully qualified name of the class which has a static main method with the signature:
   * <pre>
   *     public static void main(String args[])
   * </pre>
   * For example: {@code com.mineraltree.example.SimpleThings}
   */
  String mainClass

  /**
   * Options to provide to the JVM upon process launch. For example:
   * <pre>
   *     jvmArgs = [ '-Dmyprop=urvalue', '-Xmx4g' ]
   * </pre>
   */
  Iterable<String> jvmArgs

  EntryPoint(String name) {
    this.name = name
  }
}
