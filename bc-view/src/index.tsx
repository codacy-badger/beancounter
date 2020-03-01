import express from "express";
import logger from "./common/ConfigLogging";

// this require is necessary for server HMR to recover from error
// tslint:disable-next-line:no-var-requires
let server = require("./server").default;

if (module.hot) {
  module.hot.accept("./server", () => {
    logger.info("🔁  HMR Reloading `./server`...");
    try {
      server = require("./server").default;
    } catch (error) {
      logger.error(error);
    }
  });
  logger.info("✅  Server-side HMR is Enabled!");
}

const port = process.env.PORT || 3000;

export default express()
  .use((req, res) => server.handle(req, res))
  .listen(port, () => {
    logger.info("> Started on port %s", port);
  })
  .on("error", e => {
    logger.error(e.message);
    throw e;
  });
