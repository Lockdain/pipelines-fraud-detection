# Installing Pipelines on Azure AKS


First log into your cluster using the commands below
```
az login -u <username> -p <password>
az aks  get-credentials --resource-group [YOUR RESOURCE GROUP]  --name  [YOUR CLUSTER NAME]
```

The follow the Pipelines install instructions documented on the link below but substitute the ./install-azure script 
rather than the openshift command

https://developer.lightbend.com/docs/pipelines/1.2.0/


```
./install-azure <FQDN>
```