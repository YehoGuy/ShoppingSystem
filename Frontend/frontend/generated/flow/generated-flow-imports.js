import '@vaadin/tooltip/src/vaadin-tooltip.js';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/button/src/vaadin-button.js';
import 'Frontend/generated/jar-resources/buttonFunctions.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '641817f9b0d3ef5576daf366b19d7f9ee097097391020aaff6b24b4fdd930985') {
    pending.push(import('./chunks/chunk-0b01ede6477efc47804a185a6f28ca0cfa00887c2913e41b296a5f8849247b2d.js'));
  }
  if (key === 'ea973469eff13a45f6e31c59ad8ec99e3b5b2c0bfbc1e5218d857cdaf7533023') {
    pending.push(import('./chunks/chunk-8fb8633ddb97780f9beb2090a174dee8ee8e78a38e63850531135140c1de6d72.js'));
  }
  if (key === 'd996914dcbfc38b568afd7814e019ab86766e96190f4e59d88efe98a7a8c279f') {
    pending.push(import('./chunks/chunk-64c76ddb5c4a2e4b1abe905661d71d60e4df9bdda015ed8e41ffdf30d2ae489f.js'));
  }
  if (key === 'f30d008bb1d26e01fe7637d6e8db6af39c5d6c53260018387346396f0f59a9ab') {
    pending.push(import('./chunks/chunk-2b85016f1cabb717e4158dae3c8677ae41f9291654ee67baa2145b62651a1f1d.js'));
  }
  if (key === 'a3a799e7e42a07ee99b701800099477e8083b6af3762db0589cf493e468eb3d4') {
    pending.push(import('./chunks/chunk-2b85016f1cabb717e4158dae3c8677ae41f9291654ee67baa2145b62651a1f1d.js'));
  }
  if (key === 'c0db0049bcb60066bed76b05095310fec4252bdf4223c85d6a2d1de14f6e03b0') {
    pending.push(import('./chunks/chunk-c79ee5b9eda831f8154010fe45613c4288f1f57ab2d2c719df4ee11db7a6f4f6.js'));
  }
  if (key === '94d3b1829cdd59c5c744ed4a920f29200c5861e3952caff5df30aa26c1a30b07') {
    pending.push(import('./chunks/chunk-772a56188cc4a1dd016c1ed9ce94606632da198b1aacb10e659de160e41a6ddf.js'));
  }
  if (key === '3b909aa95b3cb6c097e3454e5671a26a615af1d7e23ebfa521b4879ba26df4dc') {
    pending.push(import('./chunks/chunk-63fd215e66f8a0d45a98077713a7e87c4056d42ae0f6fac00d0b0cad61350a87.js'));
  }
  if (key === 'ec90083a0af330e275d8181dcf17e464152e7612e56a80c988724ce569907e37') {
    pending.push(import('./chunks/chunk-0b01ede6477efc47804a185a6f28ca0cfa00887c2913e41b296a5f8849247b2d.js'));
  }
  if (key === '26cbb4f86d4fbfdb1e02f2e0207c044528f808655112a2b6aba52f233fa31008') {
    pending.push(import('./chunks/chunk-6ec4b09bc2272c622d5241fda00a2f8a72736dc770e8060e978ee6a992cb04a2.js'));
  }
  if (key === 'd58ba84471316db1ca228bb35a087c269c59fc8b7c0f63a644b2a8df92ca6437') {
    pending.push(import('./chunks/chunk-79cf43e2c79d1e729e37fda17220e9b1a3db2f209284c2fc3b62825272a5961f.js'));
  }
  if (key === 'f372d794ed5a1851df50e6ef6861d98d29d4f64da02ecfa3a01ccbf127ee3380') {
    pending.push(import('./chunks/chunk-ef4e27daf9cbae0a88187433b47dc7469392ea49169e3602ad6cf0b479cca810.js'));
  }
  if (key === '9fe6e1547b2a010f2bce66098f1b3c0e25c59153a02bdeb30c7cd6635e3630b9') {
    pending.push(import('./chunks/chunk-55278de8ec574e58235ed164a38e5aeb78edf1e76d60296dc64ef467808cb217.js'));
  }
  if (key === '4ff913e61b5b84e9091c2ace4907daf15fe52a02ddda2706c17b58e4aa47260f') {
    pending.push(import('./chunks/chunk-bcec8fa5d5640efabd3aa8a2cb6c9095f3bfa33ee424c3704c225dd93d192d4a.js'));
  }
  if (key === '516e4bd5be1c27d55b9dd04ac1115218674ceb1343717558db494c80b0d36d46') {
    pending.push(import('./chunks/chunk-9a66b73b7778cfa9c0f01358f21c29e1e39fc5856c66172393adf602308e6418.js'));
  }
  if (key === 'f0ea84aa6e30ff4c4e5c8457a87ead818651f0a343351fd30e4b140b1f9d824c') {
    pending.push(import('./chunks/chunk-672ed3e4d75c256ff431b3e4c65345dddcf19efa2ce12c12ef6a0505943643c5.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}