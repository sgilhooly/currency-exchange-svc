package com.mineraltree
/**
 * Task for cleaning Kubernetes objects outside of the generation/deployment phase.
 */
class CleanTask extends CliTask {

  @Override
  def taskApply() {
    kubectl("delete", false, ["--ignore-not-found=true", "-f", "."])
  }
}
