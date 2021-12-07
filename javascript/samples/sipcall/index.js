import SipCallSinchClientWrapper from "./SipCallSinchClientWrapper.js";
import { userId } from "../common/common.js";

const sipCallSinchClientWrapper = new SipCallSinchClientWrapper(userId());
sipCallSinchClientWrapper.start();
