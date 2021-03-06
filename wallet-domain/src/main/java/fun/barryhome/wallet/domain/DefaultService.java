package fun.barryhome.wallet.domain;


import fun.barryhome.wallet.common.enums.TradeStatus;
import fun.barryhome.wallet.common.enums.TradeType;
import fun.barryhome.wallet.common.model.TradeRecord;
import fun.barryhome.wallet.common.model.Wallet;
import fun.barryhome.wallet.domain.behavior.Behavior;
import fun.barryhome.wallet.domain.policy.CheckPolicy;
import fun.barryhome.wallet.domain.policy.CheckPolicyBuilder;
import fun.barryhome.wallet.domain.policy.NoProcessStatusAllowed;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Created on 2020/9/7 10:28 上午
 *
 * @author barry
 * Description:
 */
public abstract class DefaultService implements WalletService {

    protected abstract static class TradeConfig {
        /**
         * 交易类型
         */
        public abstract TradeType tradeType();

        /**
         * 设置行为
         */
        public abstract Behavior behavior();

        /**
         * 设置检查策略
         */
        public abstract List<CheckPolicy> checkPolicies();
    }

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private TradeRecord tradeRecord;

    @Getter(AccessLevel.PROTECTED)
    private Wallet wallet;

    @Getter(AccessLevel.PROTECTED)
    private BigDecimal tradeAmount;

    public DefaultService(TradeRecord tradeRecord) {
        this.tradeRecord = tradeRecord;

        tradeRecord.setTradeType(tradeConfig().tradeType());
        tradeRecord.setInOutFlag(tradeConfig().behavior().getInOutFlag());

        if (Strings.isEmpty(tradeRecord.getTradeNumber())) {
            tradeRecord.setTradeNumber(UUID.randomUUID().toString());
        }

        if (tradeRecord.getTradeAmount() == null) {
            tradeRecord.setTradeAmount(BigDecimal.ZERO);
        }

        wallet = tradeRecord.getWallet();
        tradeAmount = tradeRecord.getTradeAmount();
    }

    public DefaultService() {
    }

    protected abstract TradeConfig tradeConfig();


    /**
     * 执行操作
     */
    @Override
    public void done() {
        check();

        tradeConfig().behavior().doAction();

        tradeRecord.setBalance(tradeRecord.getWallet().getBalance());
        tradeRecord.setTradeStatus(TradeStatus.SUCCEED);
    }

    /**
     * 默认检查策略
     * @return
     */
    private List<CheckPolicy> defaultCheckPolicies() {
        return CheckPolicyBuilder.builder()
                .add(new NoProcessStatusAllowed(tradeRecord.getTradeStatus()))
                .build();
    }

    /**
     * 检查策略
     */
    void check() {

        List<CheckPolicy> checkPolicies = defaultCheckPolicies();
        if (tradeConfig().checkPolicies() != null) {
            checkPolicies.addAll(tradeConfig().checkPolicies());
        }

        checkPolicies.forEach(CheckPolicy::check);
    }

}
