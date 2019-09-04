package com.mineraltree

class GenerateStatefulSetDescriptor extends ControllerGenerator{

  @Override
  def generate() {
    super.generate()
    def jsonDoc = generateDatabase()
    renderJsonOutput(jsonDoc)
  }

  private def generateDatabase() {
    logger.debug "Generating database {}", getModuleFullName()
    // The 'List' kind allows you to specify multiple items in a single JSON tree
    def document = createServiceKind("v1", "List")

    document.items = [generateService(), generateStatefulSet(), generateStorageClass()]
    return document
  }

  private def generateStorageClass() {
    def document = createServiceKind("storage.k8s.io/v1", "StorageClass")
    document.metadata = [name: inputInfo.storageClass.name]
    document.provisioner = inputInfo.storageClass.provisioner
    return document
  }

  private def generateStatefulSet() {
    def document = createServiceKind("apps/v1", "StatefulSet")
    def container = generateContainer()
    container.volumeMounts = [[
        name: inputInfo.volumeMount.name,
        mountPath: inputInfo.volumeMount.path
    ]]
    container.ports = inputInfo.ports.collect { key, info -> [name: key, containerPort: info.internal] }

    def spec = [
        replicas: 1,
        serviceName: getModuleFullName(),
        selector: [
            matchLabels: [
                svc: getModuleFullName()
            ]
        ],
        template: [
            metadata: [
                labels: [
                    svc: getModuleFullName()
                ]
            ],
            spec    : [
                terminationGracePeriodSeconds: 10,
                containers: [container]
            ]
        ],
        volumeClaimTemplates: [
            [
                metadata: [
                    name: inputInfo.volumeMount.name
                ],
                spec: [
                    accessModes: [ "ReadWriteOnce" ],
                    storageClassName: inputInfo.storageClass.name,
                    resources: [
                        requests: [
                            storage: inputInfo.volumeMount.capacity
                        ]
                    ]
                ]
            ]
        ]
    ]
    document.spec = spec
    return document
  }
}
