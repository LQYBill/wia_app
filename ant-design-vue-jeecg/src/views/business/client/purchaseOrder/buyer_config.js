export default{
  buyer: {
    columns: [
      {
        title: '#',
        key: 'rowIndex',
        width: 60,
        align: 'center',
        customRender: (t, r, index) => parseInt(index) + 1
      },
      {
        title: '创建日期',
        align: 'center',
        dataIndex: 'createTime',
      },
      {
        title: '订单发票号',
        align: 'center',
        dataIndex: 'invoiceNumber',
      },
      {
        title: '客户ID',
        align: 'center',
        dataIndex: 'clientId_dictText'
      },
      {
        title: '应付金额',
        align: 'center',
        dataIndex: 'totalAmount',
      },
      {
        title: '减免总金额',
        align: 'center',
        dataIndex: 'discountAmount',
      },
      {
        title: '最终金额',
        align: 'center',
        dataIndex: 'finalAmount',
      },
      {
        title: '订单状态',
        dataIndex: 'status',
        align: 'center',
        width: 147,
        customRender: (
          t => {
            switch (t) {
              case "waitingPayment":
                return '等待支付'
              case "proofUploaded":
                return '已上传支付凭证'
              case "confirmed":
                return "已支付"
              case "purchasing":
                return "采购中"
            }
          })
      },
      {
        title: '操作',
        dataIndex: 'id',
        align: 'center',
        scopedSlots: {customRender: 'buyerAction'},
      }
    ],
    buttonDisplay:false
  },
}