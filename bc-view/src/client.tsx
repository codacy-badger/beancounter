import React, { Suspense } from "react";
import { hydrate } from "react-dom";
import { BrowserRouter, Route, Switch } from "react-router-dom";
import { useSSR } from "react-i18next";

import App from "./App";
import { KeycloakProvider } from "@react-keycloak/web";
import { getKeycloakInstance, keycloakProviderInitConfig } from "./keycloak/keycloak";

declare global {
  interface WindowI18n extends Window {
    initialI18nStore: any;
    initialLanguage: any;
  }
}

const BaseApp = (): JSX.Element => {
  useSSR((window as WindowI18n).initialI18nStore, (window as WindowI18n).initialLanguage);

  return (
    <KeycloakProvider keycloak={getKeycloakInstance} initConfig={keycloakProviderInitConfig}>
      <Suspense fallback={<div>Loading ...</div>}>
        <BrowserRouter>
          <Switch>
            <Route path="*" component={App} />
          </Switch>
        </BrowserRouter>
      </Suspense>
    </KeycloakProvider>
  );
};

hydrate(<BaseApp />, document.getElementById("root"));

if (module.hot) {
  module.hot.accept();
}
