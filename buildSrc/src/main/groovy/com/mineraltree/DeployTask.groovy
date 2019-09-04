package com.mineraltree
/**
 * Task for cleaning Kubernetes objects outside of the generation/deployment phase.
 */
class DeployTask extends CliTask {

  def deployConfigs = ["."]

  @Override
  def taskApply() {
    def args = []
    deployConfigs.each {c -> args.addAll(["-f", c]) }
    kubectl("apply", false, args)
  }
}
