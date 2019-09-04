package com.mineraltree

class GenerateNamespaceDescriptor extends BaseGenerator {

    @Override
    String getOutputFileName() {
        return "namespace.json"
    }

    @Override
    def generate() {
        super.generate()
        def configDoc = [
            kind      : "Namespace",
            apiVersion: "v1",
            metadata  : [
                name: release.getNamespace(),
                labels: [
                  name: release.getNamespace()
                ]
            ]
        ]
        renderJsonOutput(configDoc)
    }
}
