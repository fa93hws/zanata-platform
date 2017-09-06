import React, {Component} from 'react'
import PropType from 'prop-types'
import {connect} from 'react-redux'
import RejectionsForm, {MAJOR, MINOR, CRITICAL}
  from '../../components/RejectionsForm'
import Icon from '../../components/Icon'
import {Button, Accordion, Panel} from 'react-bootstrap'
import {
  fetchAllCriteria, addNewCriterion, removeCriterion
} from '../../actions/review-actions'
import {getCriteria} from '../../selectors/admin'

const DO_NOT_RENDER = undefined
/* eslint-disable max-len */
const exampleCriteria = <Accordion>
  <Panel header="Example criteria">
    <RejectionsForm
      editable={false}
      criteriaPlaceholder='Translation Errors (terminology, mistranslated, addition, omission, un-localized, do not translate, etc)'
      priority={CRITICAL} />
    <RejectionsForm
      editable
      className='active'
      criteriaPlaceholder='Language Quality (grammar, spelling, punctuation, typo, ambiguous wording, product name, sentence structuring, readability, word choice, not natural, too literal, style and tone, etc)'
      priority={MAJOR} />
    <RejectionsForm
      editable={false}
      criteriaPlaceholder='Consistency (inconsistent style or vocabulary, brand inconsistency, etc.)'
      priority={MAJOR} />
    <RejectionsForm
      editable={false}
      criteriaPlaceholder='Style Guide & Glossary Violations'
      priority={MINOR} />
    <RejectionsForm
      editable={false}
      criteriaPlaceholder='Format (mismatches, white-spaces, tag error or missing, special character, numeric format, truncated, etc.)'
      priority={MINOR} />
    <RejectionsForm
      editable
      className='active'
      criteriaPlaceholder='Other (reason may be in comment section/history if necessary)'
      priority={CRITICAL} />
  </Panel>
</Accordion>
/* eslint-enable max-len */

class AdminReview extends Component {
  static propTypes = {
    criteria: PropType.arrayOf(PropType.shape({
      editable: PropType.bool.isRequired,
      description: PropType.string.isRequired,
      priority: PropType.oneOf([MINOR, MAJOR, CRITICAL]).isRequired
    })).isRequired,
    fetchAllCriteria: PropType.func.isRequired,
    addNewEntry: PropType.func.isRequired,
    deleteEntry: PropType.func.isRequired
  }
  constructor (props) {
    super(props)
    this.state = {
      showNewEntryForm: false
    }
  }
  componentDidMount () {
    this.props.fetchAllCriteria()
  }
  addNew = () => {
    this.setState(prevState => ({
      showNewEntryForm: true
    }))
  }
  saveNewEntry = (entry) => {
    this.props.addNewEntry(entry)
  }
  render () {
    const {criteria, deleteEntry} = this.props
    const criteriaList = criteria.map((c, i) => <RejectionsForm key={i}
      editable={c.editable} entityId={c.id} onDelete={deleteEntry}
      criteriaPlaceholder={c.description}
      priority={c.priority} />)
    const newEntryForm = this.state.showNewEntryForm ? (
      <Panel header="Add new entry">
        <RejectionsForm priority={MINOR} isNewItemMode
          criteriaPlaceholder="fill in criteria"
          onSave={this.saveNewEntry} />
      </Panel>) : DO_NOT_RENDER

    return <div className='container'>
      {exampleCriteria}
      <h1>Reject translations settings</h1>
      {criteriaList}
      {newEntryForm}
      <div className='rejection-btns'>
        <Button bsStyle='primary' className='btn-left' onClick={this.addNew}>
          <Icon name='plus' className='s1' /> Add review criteria
        </Button>
        <Button bsStyle='info'>
          <Icon name='tick' className='s1' /> Save changes
        </Button>
      </div>
    </div>
  }
}

const mapStateToProps = state => {
  return {
    criteria: getCriteria(state)
  }
}

const mapDispatchToProps = dispatch => {
  return {
    fetchAllCriteria: () => dispatch(fetchAllCriteria()),
    addNewEntry: (criterion) => dispatch(addNewCriterion(criterion)),
    deleteEntry: (id) => dispatch(removeCriterion(id))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(AdminReview)
