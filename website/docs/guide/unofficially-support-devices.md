# Unofficially supported devices

::: info Non-GKI Support Note
While generic prebuilt boot images are provided for GKI 2.0+ devices, custom kernels and legacy non-GKI trees (4.14.x, 4.19.x, 5.4.x) are fully supported via source-level integration or LKM building. See the [Integrate for non-GKI Devices](how-to-integrate-for-non-gki.md) guide.
:::

::: warning
In this page, there are kernels for non-GKI devices supporting MySU maintained by other developers.
:::

::: warning
This page is intended only to help you find the source code corresponding to your device. It **DOES NOT** mean that the source code has been reviewed by MySU developers. You should use it at your own risk.
:::

<script setup>
import data from '../repos.json'
</script>

<table>
   <thead>
      <tr>
         <th>Maintainer</th>
         <th>Repository</th>
         <th>Support devices</th>
      </tr>
   </thead>
   <tbody>
    <tr v-for="repo in data" :key="repo.devices">
        <td><a :href="repo.maintainer_link" target="_blank" rel="noreferrer">{{ repo.maintainer }}</a></td>
        <td><a :href="repo.kernel_link" target="_blank" rel="noreferrer">{{ repo.kernel_name }}</a></td>
        <td>{{ repo.devices }}</td>
    </tr>
   </tbody>
</table>
