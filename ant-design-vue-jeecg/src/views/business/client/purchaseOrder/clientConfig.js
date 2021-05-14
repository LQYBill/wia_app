export default{
  client: {
    columns: [
      {
        title: '#',
        key: 'rowIndex',
        width: 60,
        align: 'center',
        customRender: (t, r, index) => parseInt(index) + 1
      },
      {
        title: 'Order Time',
        align: 'center',
        dataIndex: 'createTime'
      },
      {
        title: 'Invoice Number',
        align: 'center',
        dataIndex: 'invoiceNumber',
      },
      {
        title: 'Original price(€)',
        align: 'center',
        dataIndex: 'totalAmount',
      },
      {
        title: 'Discount(€)',
        align: 'center',
        dataIndex: 'discountAmount',
      },
      {
        title: 'Amount to be paid(€)',
        align: 'center',
        dataIndex: 'finalAmount',
      },
      {
        title: 'Status',
        align: 'center',
        dataIndex: 'status',
        key: 'status',
        width: 147,
        customRender: (
          t => {
            switch (t) {
              case "waitingPayment":
                return 'Waiting Payment'
              case "proofUploaded":
                return 'Proof uploaded'
              case "confirmed":
                return "Confirmed"
              case "purchasing":
                return "Purchasing"
            }
          })
      },
      {
        title: 'Payment Document',
        align: 'center',
        dataIndex: 'status',
        key: 'doc_status',
        scopedSlots: {customRender: 'uploadSlot'},
      }
    ],
  },
}