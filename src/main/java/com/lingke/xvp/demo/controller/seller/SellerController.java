package com.lingke.xvp.demo.controller.seller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.Rop.api.ApiException;
import com.Rop.api.request.XvpPhoneSendcodeRequest;
import com.Rop.api.request.XvpPhoneVerfiycodeRequest;
import com.Rop.api.request.XvpStoreQueryRequest;
import com.Rop.api.response.XvpPhoneSendcodeResponse;
import com.Rop.api.response.XvpPhoneVerfiycodeResponse;
import com.Rop.api.response.XvpStoreQueryResponse;
import com.lingke.xvp.demo.XvpConstants;
import com.lingke.xvp.demo.XvpRopClient;
import com.lingke.xvp.demo.controller.request.SellerRegisterRequest;
import com.lingke.xvp.demo.controller.request.SellerVerifyRequest;
import com.lingke.xvp.demo.controller.response.SellerVerifyResponse;
import com.lingke.xvp.demo.controller.response.XvpResponse;
import com.lingke.xvp.demo.db.dao.Seller;
import com.lingke.xvp.demo.utils.SessionUtil;

/**
 * Created by yuwb on 2017-03-13. 卖家相关业务处理
 */
/**
 * @author Administrator
 *
 */
@RestController("seller_seller")
@RequestMapping(value = "/seller/seller")
public class SellerController {
	@Autowired
	private XvpRopClient ropClientAdapter;

	@RequestMapping(value = "/verify", method = RequestMethod.POST)
	public XvpResponse verify(@RequestBody SellerVerifyRequest request) throws Exception {
		Seller seller = Seller.getSellerByPhone(request.getPhone());
		if (seller != null) {
			throw new RuntimeException("手机号已经被注册");
		}
		XvpPhoneSendcodeRequest ropRequest = new XvpPhoneSendcodeRequest();
		BeanUtils.copyProperties(request, ropRequest);
		ropRequest.setApp_id(ropClientAdapter.getAppId());
		XvpPhoneSendcodeResponse xvpUserCreateResponse = ropClientAdapter.ropInvoke(ropRequest);
		SellerVerifyResponse sellerVerifyResponse = new SellerVerifyResponse();
		sellerVerifyResponse.setSn(xvpUserCreateResponse.getPhoneresult().getSn());
		return sellerVerifyResponse;
	}

	@RequestMapping(value = "/register", method = RequestMethod.POST)
	public XvpResponse register(@RequestBody SellerRegisterRequest request) throws Exception {
		if (!checkCode(request)) {
			throw new RuntimeException("验证码输入错误");
		}
		Seller.addSeller(request.getPhone(), request.getPassword());
		return null;
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public XvpResponse login(@RequestBody SellerRegisterRequest request) throws Exception {
		Seller seller = Seller.getSellerByPhoneAndPassword(request.getPhone(), request.getPassword());
		if (seller == null) {
			throw new RuntimeException("用户名或者密码错误");
		}
		SessionUtil.sellerLogin(seller.getId());
		XvpStoreQueryRequest ropRequest = new XvpStoreQueryRequest();
		ropRequest.setApp_id(ropClientAdapter.getAppId());
		ropRequest.setPage_no(XvpConstants.PAGE_NO);
		ropRequest.setPage_size(ropRequest.getPage_size());
		XvpStoreQueryResponse ropResponse= ropClientAdapter.ropInvoke(ropRequest);
		if(ropResponse.getStores()!=null&&ropResponse.getStores().size()>0){
			SessionUtil.sellerSetStoreId(ropResponse.getStores().get(0).getId());
		}
		return null;
	}

	@RequestMapping(value = "/reset", method = RequestMethod.POST)
	public XvpResponse reset(@RequestBody SellerRegisterRequest request) throws Exception {
		Seller seller = Seller.getSellerByPhone(request.getPhone());
		if (seller == null) {
			throw new RuntimeException("手机号输入错误");
		}
		if (!checkCode(request)) {
			throw new RuntimeException("验证码输入错误");
		}
		Boolean isSuccess = Seller.updateSellerPasswordByPhone(request.getPhone(), request.getPassword());
		if (!isSuccess) {
			throw new RuntimeException("设置密码失败");
		}
		return null;

	}

	/**
	 * 校验验证码是否正确
	 * 
	 * @param request
	 *            用户输入信息
	 * @return
	 * @throws ApiException 
	 */
	private Boolean checkCode(SellerRegisterRequest request) throws ApiException {
		XvpPhoneVerfiycodeRequest ropRequest = new XvpPhoneVerfiycodeRequest();
		BeanUtils.copyProperties(request, ropRequest);
		ropRequest.setApp_id(ropClientAdapter.getAppId());
		XvpPhoneVerfiycodeResponse response = ropClientAdapter.ropInvoke(ropRequest);
		return Boolean.valueOf(response.getPhoneresult().getFlag());
	}
}
